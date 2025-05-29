package it.unimib.sd2025.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.Media;

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
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
import jakarta.ws.rs.core.Response.Status;

/**
 * Rappresenta la risorsa "example" in "http://localhost:8080/example".
 */
@Path("users")
public class UserResource {
    private static Map<String, User> users = new HashMap<String, User>();
    private static final float START_BALANCE = 500;

    // TODO remove this static attribute and use DB
    static {
        User user = new User();
        user.setFiscalCode("RNCNDR04T22A794U");
        user.setEmail("andrearanica2004@gmail.com");
        user.setBalance(500);
        user.setName("Andrea");
        user.setSurname("Ranica");
        user.setVouchers(new ArrayList<Voucher>());

        users.put(user.getFiscalCode(), user);

        user = new User();
        user.setFiscalCode("RSSMRA80A01B138X");
        user.setEmail("mariorossi@gmail.com");
        user.setBalance(490);
        user.setName("Mario");
        user.setSurname("Rossi");
        user.setVouchers(new ArrayList<Voucher>());

        Voucher voucher = new Voucher();
        voucher.setId(0);
        voucher.setValue(10);
        user.getVouchers().add(voucher);

        users.put(user.getFiscalCode(), user);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        return Response.ok(users).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User user) {
        // First I check that the user's attributes are valid for the request
        if (user == null || !areValuesOk(user) || !isFiscalCodeUnique(user.getFiscalCode())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (isFiscalCodeUnique(user.getFiscalCode())) {
            System.out.println("Il codice fiscale " + user.getFiscalCode() + " è univoco");
        }

        user.setBalance(START_BALANCE);

        synchronized(users) {
            users.put(user.getFiscalCode(), user);
        }

        try {
            var uri = new URI("/users/" + user.getFiscalCode());
            return Response.created(uri).build();
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        }
    }

    /**
     * Returns if the object attributes are not null
     * 
     * @param user
     * @return true if the users attributes are not null
     */
    private boolean areValuesOk(User user) {
        ArrayList<String> valuesToCheck = new ArrayList<String>();
        valuesToCheck.add(user.getFiscalCode());
        valuesToCheck.add(user.getName());
        valuesToCheck.add(user.getSurname());
        valuesToCheck.add(user.getEmail());

        for (String value : valuesToCheck) {
            if (value == null) {
                return false;
            }
        }

        return true;
    }

    public boolean isFiscalCodeUnique(String fiscalCode) {
        for (String fiscalCodeKey : users.keySet()) {
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

        synchronized (users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            return Response.ok(user).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path("/{fiscalCode}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode, String rawUser) {
        User userToModify = null;
        
        synchronized (users) {
            for (User user : users.values()) {
                if (user.getFiscalCode().equals(fiscalCode)) {
                    userToModify = user;
                }
            }
        }

        if (userToModify == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        var jsonBuilder = JsonbBuilder.create();
        try {
            var user = jsonBuilder.fromJson(rawUser, User.class);
            synchronized(userToModify) {
                userToModify.setName(user.getName());
                userToModify.setSurname(user.getSurname());
                userToModify.setEmail(user.getEmail());
            }
            return Response.ok(userToModify).build();
        } catch (JsonbException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("/{fiscalCode}/vouchers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserVouchers(@PathParam("fiscalCode") String fiscalCode) {
        User user;

        synchronized(users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            return Response.ok(user).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/{fiscalCode}/vouchers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserVoucher(@PathParam("fiscalCode") String fiscalCode, Voucher voucher) {
        User user = null;

        synchronized (users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            // I check that the given voucher has a valid value
            if (voucher.getValue() > 0 && voucher.getValue() <= user.getBalance()) {
                voucher.setId(user.getVouchers().size());
                user.setBalance(user.getBalance() - voucher.getValue());
                List<Voucher> vouchers = user.getVouchers();
                vouchers.add(voucher);
                return Response.ok(voucher).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response getUserVoucherById(@PathParam("fiscalCode") String fiscalCode, @PathParam("voucherId") int voucherId) {
        User user;

        synchronized (users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            Voucher voucher;
            synchronized (users) {
                voucher = findUserVoucherById(user, voucherId);
            }
            if (voucher != null) {
                return Response.ok(voucher).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUserVoucherById(@PathParam("fiscalCode") String fiscalCode, @PathParam("voucherId") int voucherId, Voucher voucher) {
        User user;

        synchronized (users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            Voucher voucherToChange;
            synchronized (users) {
                voucherToChange = findUserVoucherById(user, voucherId);
            }
            if (voucherToChange != null) {
                boolean valueHasToBeChanged = (voucher.getValue() != voucherToChange.getValue());
                boolean hasToBecomeConsumed = (voucher.isConsumed() && !voucherToChange.isConsumed());
                boolean newValueIsInValidRange = (voucher.getValue() > 0 && voucher.getValue() <= user.getBalance());
                boolean wantsToBecomeUnconsumed = (!voucher.isConsumed());

                // First I change the voucher value if needed
                if (valueHasToBeChanged) {
                    if (!voucherToChange.isConsumed() && newValueIsInValidRange) {
                        voucherToChange.setValue(voucher.getValue());

                        // After changing the value, I calculate the new balance of the user
                        user.setBalance(START_BALANCE);
                        for (Voucher v : user.getVouchers()) {
                            user.setBalance(user.getBalance() - v.getValue());
                        }
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }

                // Then I check if the user wants to change the consumed property
                if (voucherToChange.isConsumed() && wantsToBecomeUnconsumed) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }

                if (hasToBecomeConsumed) {
                    voucherToChange.setConsumed(true);
                }

                return Response.ok(voucherToChange).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response removeUserVoucherById(@PathParam("fiscalCode") String fiscalCode, @PathParam("voucherId") int voucherId) {
        User user;

        synchronized (users) {
            user = findUserByFiscalCode(fiscalCode);
        }

        if (user != null) {
            Voucher voucher;
            synchronized (users) {
                voucher = findUserVoucherById(user, voucherId);
            }
            if (!voucher.isConsumed()) {
                synchronized (users) {
                    user.getVouchers().remove(voucher);
                    return Response.ok().build();
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private User findUserByFiscalCode(String fiscalCode) {
        for (User user : users.values()) {
            if (user.getFiscalCode().equals(fiscalCode)) {
                return user;
            }
        }
        return null;
    }

    private Voucher findUserVoucherById(User user, int voucherId) {
        for (Voucher voucher : user.getVouchers()) {
            if (voucher.getId() == voucherId) {
                return voucher;
            }
        }
        return null;
    }
}