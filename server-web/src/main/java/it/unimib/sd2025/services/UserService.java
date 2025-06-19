package it.unimib.sd2025.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unimib.sd2025.db.IUserDao;
import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;
import it.unimib.sd2025.exceptions.UserExceptions.InvalidUserException;
import it.unimib.sd2025.exceptions.UserExceptions.UserNotFoundException;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidModifyVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidDeleteVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.VoucherNotFoundException;
import it.unimib.sd2025.exceptions.UserExceptions.InvalidModifyUserException;

public class UserService {
    private IUserDao userDao;
    private final float START_BALANCE = 500;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String[] voucherTypes = {"cinema", "musica", "concerti", "eventi", "culturali", 
                                           "libri", "musei", "strumenti musicali", "teatro", 
                                           "danza"};

    public UserService(IUserDao userDao) {
        this.userDao = userDao;
    }

    public Map<String, User> getUsers() {
        lock.readLock().lock();
        try {
            Map<String, User> users = userDao.getUsers();
            return users;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addUser(User user) throws InvalidUserException {
        if (user == null) {
            throw new InvalidUserException("User is null");
        }

        List<String> invalidAttributes = getInvalidUserAttributes(user);
        if (invalidAttributes.size() > 0) {
            throw new InvalidUserException("user fields not valid (" + invalidAttributes + ")");
        }

        user.setBalance(START_BALANCE);

        lock.writeLock().lock();
        try {
            if (!isFiscalCodeUnique(user.getFiscalCode())) {
                throw new InvalidUserException(String.format("fiscal code '%s' is already used", 
                                                             user.getFiscalCode()));
            }
            userDao.addUser(user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User getUserByFiscalCode(String fiscalCode) 
                throws UserNotFoundException {
        User user = null;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user != null) {
            return user;
        } else {
            throw new UserNotFoundException(fiscalCode);
        }
    }

    public User modifyUserByFiscalCode(String fiscalCode, User user)
                throws InvalidModifyUserException, UserNotFoundException {
        User userToModify = null;
        
        lock.writeLock().lock();

        try {
            userToModify = findUserByFiscalCode(fiscalCode);

            if (userToModify == null) {
                throw new UserNotFoundException(fiscalCode);
            }

            Map<String, String> fieldsToCheck = new HashMap<String, String>();
            fieldsToCheck.put("name", user.getName());
            fieldsToCheck.put("surname", user.getSurname());
            fieldsToCheck.put("email", user.getEmail());
            
            for (Map.Entry<String, String> entry : fieldsToCheck.entrySet()) {
                if (entry.getValue() == null) {
                    throw new InvalidModifyUserException(String.format("'%s' cannot be 'null'", 
                                                                       entry.getKey()));
                }
            }

            userToModify.setName(user.getName());
            userToModify.setSurname(user.getSurname());
            userToModify.setEmail(user.getEmail());
    
            userDao.modifyUser(userToModify);

            return userToModify;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Voucher> getUserVouchers(String fiscalCode) throws UserNotFoundException {
        User user;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user == null) {
            throw new UserNotFoundException(fiscalCode);
        }

        return user.getVouchers();
    }

    public Voucher addVoucherToUser(String fiscalCode, Voucher voucher) 
                   throws UserNotFoundException, InvalidVoucherException {
        User user = null;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user == null) {
            throw new UserNotFoundException(fiscalCode);
        }

        if (voucher == null) {
            throw new InvalidVoucherException("Voucher is null");
        }

        List<String> invalidAttributes = getInvalidVoucherAttributes(voucher);
        if (invalidAttributes.size() > 0) {
            throw new InvalidVoucherException(String.format("Invalid attribute '%s'", 
                                                            invalidAttributes.get(0)));
        }

        if (voucher.getValue() <= user.getBalance()) {
            int maxVoucherId = -1;
            lock.writeLock().lock();
            try {
                for (Voucher v : user.getVouchers()) {
                    if (v.getId() > maxVoucherId) {
                        maxVoucherId = v.getId();
                    }
                }

                voucher.setId(maxVoucherId + 1);

                user.setBalance(user.getBalance() - voucher.getValue());
                userDao.addVoucherToUser(voucher, user);
                
                userDao.modifyUser(user);
            } finally {
                lock.writeLock().unlock();
            }

            return voucher;
        } else {
            String exceptionMessage = "Voucher value is greather than the user balance (%f > %f)";
            throw new InvalidVoucherException(String.format(exceptionMessage,
                                                            voucher.getValue(),
                                                            user.getBalance()));
        }
    }

    public Voucher getUserVoucherById(String fiscalCode, int voucherId) 
                   throws UserNotFoundException, VoucherNotFoundException {
        User user;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user == null) {
            throw new UserNotFoundException(fiscalCode);
        }

        Voucher voucher;
        lock.readLock().lock();
        try {
            voucher = findUserVoucherById(user, voucherId);
        } finally {
            lock.readLock().unlock();
        }

        if (voucher == null) {
            throw new VoucherNotFoundException(fiscalCode, voucherId);
        }

        return voucher;
    }

    public Voucher modifyUserVoucherById(String fiscalCode, int voucherId, Voucher newVoucher)
                   throws UserNotFoundException, VoucherNotFoundException, InvalidVoucherException,
                   InvalidModifyVoucherException {
        
        List<String> invalidAttributes = getInvalidVoucherAttributes(newVoucher);
        if (invalidAttributes.size() > 0) {
            String errorMessage = String.format("New voucher attribute '%s' is not valid",
                                                invalidAttributes.get(0));
            throw new InvalidVoucherException(errorMessage);
        }

        lock.writeLock().lock();
        try {
            User user = findUserByFiscalCode(fiscalCode);

            if (user == null) {
                throw new UserNotFoundException(fiscalCode);
            }

            Voucher originalVoucher;
            originalVoucher = findUserVoucherById(user, voucherId);

            if (originalVoucher == null) {
                throw new VoucherNotFoundException(fiscalCode, voucherId);
            }

            List<String> invalidChanges = getNewVoucherInvalidChanges(originalVoucher, newVoucher, 
                                                                      user);
            if (invalidChanges.size() > 0) {
                throw new InvalidModifyVoucherException(invalidChanges.get(0));
            }

            if (!originalVoucher.getType().equals(newVoucher.getType())) {
                originalVoucher.setType(newVoucher.getType());
            }
            
            if (!originalVoucher.isConsumed() && newVoucher.isConsumed()) {
                originalVoucher.setConsumed(newVoucher.isConsumed());
                originalVoucher.setConsumedDateTime(newVoucher.getConsumedDateTime());
            }

            userDao.modifyUserVoucher(originalVoucher, user);

            return originalVoucher;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteUserVoucherById(String fiscalCode, int voucherId) 
                throws UserNotFoundException, VoucherNotFoundException, 
                       InvalidDeleteVoucherException {
        lock.writeLock().lock();
        try {
            User user = findUserByFiscalCode(fiscalCode);
            if (user == null) {
                throw new UserNotFoundException(fiscalCode);
            }
            
            Voucher voucher;
            voucher = findUserVoucherById(user, voucherId);
            if (voucher == null) {
                throw new VoucherNotFoundException(fiscalCode, voucherId);
            }
        
            if (voucher.isConsumed()) {
                throw new InvalidDeleteVoucherException(fiscalCode, voucherId);
            }
            userDao.deleteUserVoucher(voucher, user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<String> getInvalidUserAttributes(User user) {
        List<String> invalidAttributes = new ArrayList<String>();

        Map<String, String> valuesToCheckAreNotNull = new HashMap<String, String>();
        valuesToCheckAreNotNull.put("fiscal code", user.getFiscalCode());
        valuesToCheckAreNotNull.put("name", user.getName());
        valuesToCheckAreNotNull.put("surname", user.getSurname());
        valuesToCheckAreNotNull.put("email", user.getEmail());
        
        for (Map.Entry<String, String> entry : valuesToCheckAreNotNull.entrySet()) {
            if (entry.getValue() == null) {
                invalidAttributes.add(entry.getKey());
            }
        }

        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(user.getEmail());

        if (!mat.find()) {
            invalidAttributes.add("email");
        }

        return invalidAttributes;
    }

    public boolean isFiscalCodeUnique(String fiscalCode) {
        for (String fiscalCodeKey : userDao.getUsers().keySet()) {
            if (fiscalCodeKey.equals(fiscalCode)) {
                return false;
            }
        }
        return true;
    }

    private User findUserByFiscalCode(String fiscalCode) {
        for (User user : userDao.getUsers().values()) {
            if (user.getFiscalCode().equals(fiscalCode)) {
                return user;
            }
        }
        return null;
    }

    private List<String> getInvalidVoucherAttributes(Voucher voucher) {
        List<String> invalidAttributes = new ArrayList<String>();

        Map<String, String> valuesToCheckAreNotNull = new HashMap<String, String>();
        valuesToCheckAreNotNull.put("type", voucher.getType());
        valuesToCheckAreNotNull.put("createdDateTime", voucher.getCreatedDateTime());
        
        for (Map.Entry<String, String> entry : valuesToCheckAreNotNull.entrySet()) {
            if (entry.getValue() == null) {
                invalidAttributes.add(entry.getKey());
            }
        }
        
        if (voucher.getValue() <= 0) {
            invalidAttributes.add("value");
        }

        if (!invalidAttributes.contains("createdDateTime")) {
            if (!isDateTimeCorrect(voucher.getCreatedDateTime())) {
                invalidAttributes.add("createdDateTime");
            }    
        }
        
        if (!invalidAttributes.contains("type")) {
            boolean isVoucherTypeValid = false;
            for (String voucherType : voucherTypes) {
                if (voucherType.toUpperCase().equals(voucher.getType().toUpperCase())) {
                    isVoucherTypeValid = true;
                }
            }
            
            if (!isVoucherTypeValid) {
                invalidAttributes.add("type");
            }
        }

        return invalidAttributes;
    }

    private boolean isDateTimeCorrect(String dateTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        dateFormat.setLenient(false);

        try {
            dateFormat.parse(dateTime.trim());
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    private Voucher findUserVoucherById(User user, int voucherId) {
        for (Voucher voucher : user.getVouchers()) {
            if (voucher.getId() == voucherId) {
                return voucher;
            }
        }
        return null;
    }

    List<String> getNewVoucherInvalidChanges(Voucher originalVoucher, Voucher newVoucher, 
                                             User user) {
        List<String> invalidChanges = new ArrayList<String>();
        
        boolean createdDateHasChanged = (newVoucher.getCreatedDateTime() != null && !newVoucher.getCreatedDateTime().equals(originalVoucher.getCreatedDateTime()));
        boolean consumedDateHasChanged = (originalVoucher.getConsumedDateTime() != null && newVoucher.getConsumedDateTime() != null && !newVoucher.getConsumedDateTime().equals(originalVoucher.getConsumedDateTime()));

        if (originalVoucher.isConsumed() && !newVoucher.isConsumed()) {
            invalidChanges.add("cannot change voucher 'consumed' if it has already been consumed");
        }

        if (createdDateHasChanged) {
            invalidChanges.add("'createdDateTime' cannot be changed");
        }

        if (consumedDateHasChanged) {
            invalidChanges.add("'consumedDateTime' cannot be changed");
        }

        if (newVoucher.getValue() != originalVoucher.getValue()) {
            invalidChanges.add("cannot change voucher value");
        }

        if (!originalVoucher.isConsumed() && newVoucher.isConsumed() && newVoucher.getConsumedDateTime() == null) {
            invalidChanges.add("'consumedDateTime' cannot be null if voucher becomes consumed");
        }

        if (!originalVoucher.getType().equals(newVoucher.getType()) && originalVoucher.isConsumed()) {
            invalidChanges.add("cannot change voucher type if voucher has been consumed");
        }

        return invalidChanges;
    }
}
