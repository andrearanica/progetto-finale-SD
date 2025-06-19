package it.unimib.sd2025.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;
import it.unimib.sd2025.services.UserService;
import it.unimib.sd2025.exceptions.UserExceptions.*;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidDeleteVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidModifyVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.InvalidVoucherException;
import it.unimib.sd2025.exceptions.VoucherExceptions.VoucherNotFoundException;
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

@Path("users")
public class UserResource {
    private final UserService userService = new UserService(new UserDaoTcp("localhost", 
                                                                           3030));

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        try {
            Map<String, User> users = userService.getUsers();
            return Response.ok(users).build();
        } catch (Exception e) {
            String errorMessage = "Internal Server Error trying to get all users";
            return getServerErrorResponse(errorMessage);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User user) {
        try {
            userService.addUser(user);
            var uri = new URI("/users/" + user.getFiscalCode());
            return Response.created(uri).build();
        } catch (InvalidUserException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        } catch (Exception e) {
            String errorMessage = "Internal Server Error trying to add user";
            return getServerErrorResponse(errorMessage);
        }
    }

    @Path("/{fiscalCode}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode) {
        try {
            User user = userService.getUserByFiscalCode(fiscalCode);
            return Response.ok(user).build();
        } catch (UserNotFoundException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            String errorMessage = "Internal Server Error while searching user";
            return getServerErrorResponse(errorMessage);
        }
    }

    @Path("/{fiscalCode}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode, User user) {
        try {
            User modifiedUser = userService.modifyUserByFiscalCode(fiscalCode, user);
            return Response.ok(modifiedUser).build();
        } catch (UserNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (InvalidModifyUserException e) {
            return getBadRequestResponse(e.getMessage());
        }
    } 

    @GET
    @Path("/{fiscalCode}/vouchers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserVouchers(@PathParam("fiscalCode") String fiscalCode) {
        try {
            List<Voucher> vouchers = userService.getUserVouchers(fiscalCode);
            return Response.ok(vouchers).build();
        } catch (UserNotFoundException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            String errorMessage = "Internal Server Error while getting the user vouchers";
            return getServerErrorResponse(errorMessage);
        }
    }

    @POST
    @Path("/{fiscalCode}/vouchers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addVoucherToUser(@PathParam("fiscalCode") String fiscalCode, Voucher voucher) {
        try {
            Voucher createdVoucher = userService.addVoucherToUser(fiscalCode, voucher);
            var uri = new URI("/users/" + fiscalCode + "/vouchers/" + createdVoucher.getId());
            return Response.created(uri).build();
        } catch (UserNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (InvalidVoucherException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            String errorMessage = "Internal Server Error while adding voucher to user";
            return getServerErrorResponse(errorMessage);
        }
    }

    @GET
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response getUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                       @PathParam("voucherId") int voucherId) {
        try {
            Voucher voucher = userService.getUserVoucherById(fiscalCode, voucherId);
            return Response.ok(voucher).build();
        } catch (UserNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (VoucherNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        }
    }

    @PUT
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                          @PathParam("voucherId") int voucherId, Voucher newVoucher) {
        try {
            Voucher modifiedVoucher = userService.modifyUserVoucherById(fiscalCode, voucherId, 
                                                                        newVoucher);
            return Response.ok(modifiedVoucher).build();
        } catch (UserNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (VoucherNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (InvalidVoucherException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (InvalidModifyVoucherException e) {
            return getBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            String errorMessage = "Internal Server Error while modifying user voucher";
            return getServerErrorResponse(errorMessage);
        }
    }

    @DELETE
    @Path("/{fiscalCode}/vouchers/{voucherId}")
    public Response deleteUserVoucherById(@PathParam("fiscalCode") String fiscalCode, 
                                          @PathParam("voucherId") int voucherId) {
        try {
            userService.deleteUserVoucherById(fiscalCode, voucherId);
            return Response.ok().build();
        } catch (UserNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (VoucherNotFoundException e) {
            return getNotFoundResponse(e.getMessage());
        } catch (InvalidDeleteVoucherException e) {
            return getBadRequestResponse(e.getMessage());
        }
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

    private Response getServerErrorResponse(String errorMessage) {
        String responseBody = String.format("{\"error\": \"%s\"}", errorMessage);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(responseBody)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}