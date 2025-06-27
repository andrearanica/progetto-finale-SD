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
import java.util.Date;

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
    private final String[] voucherTypes = {"cinema", "musica", "concerti", "eventi culturali", 
                                           "libri", "musei", "strumenti musicali", "teatro",
                                           "danza"};

    public UserService(IUserDao userDao) {
        this.userDao = userDao;
    }

    public List<User> getAllUsers() {
        synchronized (userDao) {
            List<User> users = userDao.getAllUsers();
            return users;
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

        synchronized (userDao) {
            if (!isFiscalCodeUnique(user.getFiscalCode())) {
                throw new InvalidUserException(String.format("fiscal code '%s' is already used",
                                                             user.getFiscalCode()));
            }
            userDao.addUser(user);
        }
    }

    public User getUserByFiscalCode(String fiscalCode)
                throws UserNotFoundException {
        User user = null;

        synchronized (userDao) {
            user = findUserByFiscalCode(fiscalCode);
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

        synchronized (userDao) {
            userToModify = findUserByFiscalCode(fiscalCode);

            if (userToModify == null) {
                throw new UserNotFoundException(fiscalCode);
            }

            Map<String, String> fieldsToCheck = new HashMap<String, String>();
            fieldsToCheck.put("name", user.getName());
            fieldsToCheck.put("surname", user.getSurname());
            fieldsToCheck.put("email", user.getEmail());
            fieldsToCheck.put("fiscalCode", user.getFiscalCode());
            
            for (Map.Entry<String, String> entry : fieldsToCheck.entrySet()) {
                if (entry.getValue() == null) {
                    throw new InvalidModifyUserException(String.format("'%s' cannot be 'null'",
                                                                       entry.getKey()));
                }
            }

            if (userToModify.getBalance() != user.getBalance()) {
                throw new InvalidModifyUserException("balance");
            }
            if (!userToModify.getFiscalCode().equals(user.getFiscalCode())) {
                throw new InvalidModifyUserException("fiscalCode");
            }

            List<Voucher> oldVouchers = userToModify.getVouchers();
            List<Voucher> newVouchers = user.getVouchers();

            if (newVouchers != null) {
                if (oldVouchers.size() != newVouchers.size()) {
                    throw new InvalidModifyUserException("vouchers");
                }

                for (int i = 0; i < oldVouchers.size(); i++) {
                    if (!oldVouchers.get(i).equals(newVouchers.get(i))) {
                        throw new InvalidModifyUserException("vouchers");
                    }
                }
            }

            userToModify.setName(user.getName());
            userToModify.setSurname(user.getSurname());
            userToModify.setEmail(user.getEmail());

            userDao.modifyUser(userToModify);
        }
        return userToModify;
    }

    public List<Voucher> getUserVouchers(String fiscalCode) throws UserNotFoundException {
        User user;

        synchronized (userDao) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user == null) {
            throw new UserNotFoundException(fiscalCode);
        }

        return user.getVouchers();
    }

    public Voucher addVoucherToUser(String fiscalCode, Voucher voucher)
                   throws UserNotFoundException, InvalidVoucherException {
        User user = null;

        synchronized (userDao) {
            user = findUserByFiscalCode(fiscalCode);
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
            synchronized (userDao) {
                for (Voucher v : user.getVouchers()) {
                    if (v.getId() > maxVoucherId) {
                        maxVoucherId = v.getId();
                    }
                }

                voucher.setId(maxVoucherId + 1);

                user.setBalance(user.getBalance() - voucher.getValue());
                userDao.addVoucherToUser(voucher, user);

                userDao.modifyUser(user);
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

        synchronized (userDao) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user == null) {
            throw new UserNotFoundException(fiscalCode);
        }

        Voucher voucher;
        synchronized (userDao) {
            voucher = findUserVoucherById(user, voucherId);
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

        synchronized (userDao) {
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
        }
    }

    public void deleteUserVoucherById(String fiscalCode, int voucherId)
                throws UserNotFoundException, VoucherNotFoundException,
                       InvalidDeleteVoucherException {
        synchronized (userDao) {
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

            user.getVouchers().remove(voucher);
            user.setBalance(START_BALANCE);
            for (Voucher v : user.getVouchers()) {
                user.setBalance(user.getBalance() - v.getValue());
            }
            userDao.modifyUser(user);
        }
    }

    private List<String> getInvalidUserAttributes(User user) {
        List<String> invalidAttributes = new ArrayList<String>();

        Map<String, String> valuesToCheckAreNotNull = new HashMap<String, String>();
        valuesToCheckAreNotNull.put("fiscalCode", user.getFiscalCode());
        valuesToCheckAreNotNull.put("name", user.getName());
        valuesToCheckAreNotNull.put("surname", user.getSurname());
        valuesToCheckAreNotNull.put("email", user.getEmail());

        for (Map.Entry<String, String> entry : valuesToCheckAreNotNull.entrySet()) {
            if (entry.getValue() == null) {
                invalidAttributes.add(entry.getKey());
            }
        }

        if (!invalidAttributes.contains("email")) {
            Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
            Matcher matcher = pattern.matcher(user.getEmail());

            if (!matcher.find()) {
                invalidAttributes.add("email");
            }
        }

        if (!invalidAttributes.contains("fiscalCode")) {
            Pattern pattern = Pattern.compile("[A-Z][A-Z][A-Z][A-Z][A-Z][A-Z][0-9][0-9][A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z]");
            Matcher matcher = pattern.matcher(user.getFiscalCode());
            if (!matcher.find()) {
                invalidAttributes.add("fiscalCode");
            }
        }

        return invalidAttributes;
    }

    public boolean isFiscalCodeUnique(String fiscalCode) {
        for (User user : userDao.getAllUsers()) {
            if (user.getFiscalCode().equals(fiscalCode)) {
                return false;
            }
        }
        return true;
    }

    private User findUserByFiscalCode(String fiscalCode) {
        for (User user : userDao.getAllUsers()) {
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
            Date date = dateFormat.parse(dateTime.trim());
            if (date.after(new Date())) {
                return false;
            }
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

        if (originalVoucher.getId() != newVoucher.getId()) {
            invalidChanges.add("cannot change voucher 'id' attribute");
        }

        if (originalVoucher.isConsumed() && !newVoucher.isConsumed()) {
            invalidChanges.add("cannot change voucher 'consumed' if it has already been consumed");
        }

        if (createdDateHasChanged) {
            invalidChanges.add("'createdDateTime' cannot be changed");
        }

        if (consumedDateHasChanged) {
            invalidChanges.add("'consumedDateTime' cannot be changed");
        }

        if (newVoucher.getConsumedDateTime() != null && newVoucher.isConsumed() && !isDateTimeCorrect(newVoucher.getConsumedDateTime())) {
            invalidChanges.add("new voucher attribute 'consumedDateTime' is not correct");
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

        if (newVoucher.getConsumedDateTime()!= null && !isConsumedDateTimeOk(newVoucher.getConsumedDateTime(), originalVoucher.getCreatedDateTime())) {
            invalidChanges.add("'consumedDateTime' must be after 'createdDateTime'");
        }

        return invalidChanges;
    }

    private boolean isConsumedDateTimeOk(String consumedDateTimeRaw, String createdDateTimeRaw) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        dateFormat.setLenient(false);
        
        try {
            Date consumedDateTime = dateFormat.parse(consumedDateTimeRaw.trim());
            Date createdDateTime = dateFormat.parse(createdDateTimeRaw.trim());
            return consumedDateTime.after(createdDateTime);
        } catch (ParseException e) {
            return false;
        }
    }
}
