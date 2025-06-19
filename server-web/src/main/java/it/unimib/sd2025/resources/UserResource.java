package it.unimib.sd2025.resources;

import java.net.URI;
import java.net.URISyntaxException;
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

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;
import it.unimib.sd2025.db.IUserDao;
import it.unimib.sd2025.db.UserDaoTcp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rappresenta la risorsa "User" in "http://localhost:8080/users".
 */
@Path("users")
public class UserResource {
    private IUserDao userDao = new UserDaoTcp("localhost", 3030);
    private final float START_BALANCE = 500;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String[] voucherTypes = {"cinema", "musica", "concerti", "eventi", "culturali", 
                                           "libri", "musei", "strumenti musicali", "teatro", "danza"};

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        lock.readLock().lock();
        try {
            Map<String, User> users = userDao.getUsers();
            return Response.ok(users).build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User user) {
        // First I check that the user's attributes are valid for the request
        if (user == null) {
            return getBadRequestResponse("user is null");
        }

        List<String> invalidAttributes = getInvalidUserAttributes(user);
        if (invalidAttributes.size() > 0) {
            return getBadRequestResponse("user fields not valid (" + invalidAttributes + ")");
        }

        user.setBalance(START_BALANCE);

        lock.writeLock().lock();
        try {
            // If the given user has a not valid fiscal code, 
            if (!isFiscalCodeUnique(user.getFiscalCode())) {
                return getBadRequestResponse("fiscal code is already used");
            }
            userDao.addUser(user);
        } finally {
            lock.writeLock().unlock();
        }

        try {
            var uri = new URI("/users/" + user.getFiscalCode());
            return Response.created(uri).build();
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        }
    }

    /**
     * Returns true if the object attributes are not null
     * 
     * @param user
     * @return true if the users attributes are not null
     */
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

    /**
     * Returns true if the given fiscal code hasn't already been used in the DB
     * 
     * @param fiscalCode
     * @return bool
     */
    public boolean isFiscalCodeUnique(String fiscalCode) {
        for (String fiscalCodeKey : userDao.getUsers().keySet()) {
            if (fiscalCodeKey.equals(fiscalCode)) {
                return false;
            }
        }
        return true;
    }

    @Path("/{fiscalCode}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode) {
        User user = null;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user != null) {
            return Response.ok(user).build();
        } else {
            return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
        }
    }

    @Path("/{fiscalCode}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode, User user) {
        User userToModify = null;
        
        lock.writeLock().lock();

        try {
            userToModify = findUserByFiscalCode(fiscalCode);

            if (userToModify == null) {
                return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
            }
    
            userToModify.setName(user.getName());
            userToModify.setSurname(user.getSurname());
            userToModify.setEmail(user.getEmail());
    
            // Then I change the user attributes in the DB
            userDao.modifyUser(userToModify);
        } finally {
            lock.writeLock().unlock();
        }

        return Response.ok(userToModify).build();
    }

    @GET
    @Path("/{fiscalCode}/vouchers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserVouchers(@PathParam("fiscalCode") String fiscalCode) {
        User user;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user != null) {
            return Response.ok(user.getVouchers()).build();
        } else {
            return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
        }
    }

    @POST
    @Path("/{fiscalCode}/vouchers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserVoucher(@PathParam("fiscalCode") String fiscalCode, Voucher voucher) {
        // First I check if the voucher is valid
        if (voucher == null) {
            return getBadRequestResponse("voucher is null");
        }

        List<String> invalidAttributes = getInvalidVoucherAttributes(voucher);
        if (invalidAttributes.size() > 0) {
            return getBadRequestResponse("voucher attributes are not valid (" + invalidAttributes + ")");
        }

        User user = null;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user != null) {
            // I check that the given voucher has a valid value
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
                
                return Response.ok(voucher).build();
            } else {
                return getBadRequestResponse(String.format("voucher value is greather than the user balance (%f > %f)",
                                             voucher.getValue(),
                                             user.getBalance()));
            }
        } else {
            return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
        }
    }

    @GET
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response getUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                       @PathParam("voucherId") int voucherId) {
        User user;

        lock.readLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);
        } finally {
            lock.readLock().unlock();
        }

        if (user != null) {
            Voucher voucher;
            lock.readLock().lock();
            try {
                voucher = findUserVoucherById(user, voucherId);
            } finally {
                lock.readLock().unlock();
            }

            if (voucher != null) {
                return Response.ok(voucher).build();
            } else {
                return getNotFoundResponse(String.format("cannot find voucher with ID '%d' in user '%s'", voucherId, fiscalCode));
            }
        } else {
            return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
        }
    }

    @PUT
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                          @PathParam("voucherId") int voucherId, Voucher newVoucher) {
        User user;

        List<String> invalidAttributes = getInvalidVoucherAttributes(newVoucher);
        if (invalidAttributes.size() > 0) {
            return getBadRequestResponse("voucher attributes not valid (" + invalidAttributes + ")");
        }

        lock.writeLock().lock();
        try {
            user = findUserByFiscalCode(fiscalCode);

            if (user != null) {
                Voucher originalVoucher;
                originalVoucher = findUserVoucherById(user, voucherId);

                if (originalVoucher == null) {
                    return getNotFoundResponse(String.format("cannot find voucher with ID '%d' in user '%s'", voucherId, fiscalCode));
                }

                List<String> invalidChanges = getNewVoucherInvalidChanges(originalVoucher, newVoucher, user);
                if (invalidChanges.size() > 0) {
                    return getBadRequestResponse(String.format("invalid requests (" + invalidChanges + ")"));
                }

                if (!originalVoucher.getType().equals(newVoucher.getType())) {
                    originalVoucher.setType(newVoucher.getType());
                }
                
                if (!originalVoucher.isConsumed() && newVoucher.isConsumed()) {
                    originalVoucher.setConsumed(newVoucher.isConsumed());
                    originalVoucher.setConsumedDateTime(newVoucher.getConsumedDateTime());
                }

                userDao.modifyUserVoucher(originalVoucher, user);

                return Response.ok(originalVoucher).build();
            } else {
                return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @DELETE
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response removeUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                          @PathParam("voucherId") int voucherId) {
        lock.writeLock().lock();
        try {
            User user = findUserByFiscalCode(fiscalCode);
        
            if (user != null) {
                Voucher voucher;
                voucher = findUserVoucherById(user, voucherId);
                if (voucher != null) {
                    if (!voucher.isConsumed()) {
                        userDao.deleteUserVoucher(voucher, user);
                        return Response.ok().build();
                    } else {
                        return getBadRequestResponse("cannot delete used voucher because it has already been consumed");
                    }
                } else {
                    return getNotFoundResponse(String.format("cannot find voucher with ID '%d' in user '%s'", voucherId, fiscalCode));
                }
            } else {
                return getNotFoundResponse(String.format("cannot find user with fiscal code '%s'", fiscalCode));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the user that has the given fiscal code in the DB
     * 
     * @param fiscalCode
     * @return User
     */
    private User findUserByFiscalCode(String fiscalCode) {
        for (User user : userDao.getUsers().values()) {
            if (user.getFiscalCode().equals(fiscalCode)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Returns the voucher of a specific user that has a specific ID
     * 
     * @param user
     * @param voucherId
     * @return Voucher
     */
    private Voucher findUserVoucherById(User user, int voucherId) {
        for (Voucher voucher : user.getVouchers()) {
            if (voucher.getId() == voucherId) {
                return voucher;
            }
        }
        return null;
    }

    /**
     * Returns true if the voucher attributes are legit, so not null
     * 
     * @param voucher
     * @return boolean
     */
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
        if (!isDateTimeCorrect(voucher.getCreatedDateTime())) {
            invalidAttributes.add("createdDateTime");
        }
        
        // Finally I check if the given voucher type is in the valid ones
        boolean isVoucherTypeValid = false;
        for (String voucherType : voucherTypes) {
            if (voucherType.toUpperCase().equals(voucher.getType().toUpperCase())) {
                isVoucherTypeValid = true;
            }
        }

        if (!isVoucherTypeValid) {
            invalidAttributes.add("type");
        }

        return invalidAttributes;
    }

    /**
     * Returns true if the new voucher has some changes that can be done correctly on the old one
     * 
     * @param originalVoucher
     * @param newVoucher
     * @param user
     * @return boolean
     */
    List<String> getNewVoucherInvalidChanges(Voucher originalVoucher, Voucher newVoucher, User user) {
        List<String> invalidChanges = new ArrayList<String>();
        
        // FIXME pretty orrible
        boolean wantsToBecomeUnconsumed = (originalVoucher.isConsumed() && !newVoucher.isConsumed());
        boolean createdDateHasChanged = (originalVoucher.getCreatedDateTime() != null && newVoucher.getCreatedDateTime() != null && !newVoucher.getCreatedDateTime().equals(originalVoucher.getCreatedDateTime()));
        boolean consumedDateHasChanged = (originalVoucher.getConsumedDateTime() != null && newVoucher.getConsumedDateTime() != null && !newVoucher.getConsumedDateTime().equals(originalVoucher.getConsumedDateTime()));

        // The voucher cannot become unconsumed if it is consumed
        if (originalVoucher.isConsumed() && wantsToBecomeUnconsumed) {
            invalidChanges.add("cannot change voucher 'consumed' if it has already been consumed");
        }

        // The created date can't be changed
        if (createdDateHasChanged) {
            invalidChanges.add("createdDateTime cannot be changed");
        }

        // If the consumed date is set it can't be changed
        if (consumedDateHasChanged) {
            invalidChanges.add("consumedDateTime cannot be changed");
        }

        // If the new voucher has a consumed date but it's not consumed, it is not valid
        if (!newVoucher.isConsumed() && newVoucher.getConsumedDateTime() != null) {
            invalidChanges.add("consumedDateTime can only be set if voucher becomes consumed");
        }

        if (newVoucher.getValue() != originalVoucher.getValue()) {
            invalidChanges.add("cannot change voucher value");
        }

        if (!originalVoucher.isConsumed() && newVoucher.isConsumed() && newVoucher.getConsumedDateTime() == null) {
            invalidChanges.add("consumedDateTime cannot be null if voucher becomes consumed");
        }

        if (!originalVoucher.getType().equals(newVoucher.getType()) && originalVoucher.isConsumed()) {
            invalidChanges.add("cannot change voucher type if voucher has been consumed");
        }

        return invalidChanges;
    }

    private Response getBadRequestResponse(String errorMessage) {
        String responseBody = String.format("{\"error\": \"%s\"}", errorMessage);
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(responseBody)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    private Response getNotFoundResponse(String errorMessage) {
        String responseBody = String.format("{\"error\": \"%s\"}", errorMessage);
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(responseBody)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
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
}