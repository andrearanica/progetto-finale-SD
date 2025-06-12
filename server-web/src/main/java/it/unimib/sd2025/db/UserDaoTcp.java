package it.unimib.sd2025.db;

import it.unimib.sd2025.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserDaoTcp implements IUserDao {
    private String address;
    private int port;
    
    public UserDaoTcp(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public Map<String, User> getUsers() {
        List<String> fiscalCodes = getFiscalCodes();

        Map<String, User> users = new HashMap<String, User>();

        for (String fiscalCode : fiscalCodes) {
            System.out.println(fiscalCode);
            String userName = getUserProperty(fiscalCode, "name");
            String userSurname = getUserProperty(fiscalCode, "surname");
            String userEmail = getUserProperty(fiscalCode, "email");
            String userBalanceRaw = getUserProperty(fiscalCode, "balance");
            List<Voucher> userVouchers = getUserVouchers(fiscalCode);

            User user = new User();
            user.setFiscalCode(fiscalCode);
            user.setName(userName);
            user.setSurname(userSurname);
            user.setEmail(userEmail);
            user.setBalance(Float.parseFloat(userBalanceRaw.replace(",", ".")));
            user.setVouchers(userVouchers);

            users.put(fiscalCode, user);
        }

        return users;
    }

    /**
     * Returns all the fiscal codes saved inside the DB
     * 
     * @param socketInputStream
     * @param socketOutputStream
     * @return
     */
    public List<String> getFiscalCodes() {
        String rawFiscalCodes = executeDBCommand("GETL fiscalCodes");
        String[] fiscalCodesWithOk = rawFiscalCodes.split(" ");

        // I remove the first item because the response is OK + fiscal codes
        String[] fiscalCodes = Arrays.copyOfRange(fiscalCodesWithOk, 1, fiscalCodesWithOk.length);

        return new ArrayList<String>(Arrays.asList(fiscalCodes));
    }

    /**
     * Returns the property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public String getUserProperty(String fiscalCode, String property) {
        String serverResponse = executeDBCommand(String.format("GET %s.%s", fiscalCode, property));
        if (serverResponse.equals("OK ")) {
            return null;
        }
        return serverResponse.split(" ")[1];
    }

    /**
     * Returns the list property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property the key of the list in the DB
     * @return
     */
    public List<Voucher> getUserVouchers(String fiscalCode) {
        List<Voucher> vouchers = new ArrayList<Voucher>();

        String vouchersIdsRaw = executeDBCommand(String.format("GETL %s.vouchersIds", fiscalCode));
        String[] vouchersIdsWithOk = vouchersIdsRaw.split(" ");
        String[] vouchersIds = Arrays.copyOfRange(vouchersIdsWithOk, 1, vouchersIdsWithOk.length);

        for (String voucherIdRaw : vouchersIds) {
            int voucherId = Integer.parseInt(voucherIdRaw);

            String voucherValueRaw = getVoucherProperty(fiscalCode, voucherId, "value");
            String voucherConsumedRaw = getVoucherProperty(fiscalCode, voucherId, "consumed");
            String voucherType = getVoucherProperty(fiscalCode, voucherId, "type");
            String voucherCreatedDateTime = getVoucherProperty(fiscalCode, voucherId, "createdDateTime");
            String voucherConsumedDateTime = getVoucherProperty(fiscalCode, voucherId, "consumedDateTime");

            // FIXME how to check inconsistencies?
            if (voucherValueRaw == null || voucherConsumedRaw == null || voucherType == null || voucherCreatedDateTime == null) {
                System.out.println(String.format("[DEBUG] DB inconsistency: missing data for voucher %d of the user %s", 
                                                 voucherId, fiscalCode));
                continue;
            }

            Voucher voucher = new Voucher();
            voucher.setId(voucherId);
            voucher.setValue(Float.parseFloat(voucherValueRaw.replace(",", ".")));
            voucher.setConsumed(Boolean.parseBoolean(voucherConsumedRaw));
            voucher.setType(voucherType);
            
            voucher.setCreatedDateTime(voucherCreatedDateTime);
            if (voucherConsumedDateTime != null) {
                voucher.setConsumedDateTime(voucherConsumedDateTime);
            }

            vouchers.add(voucher);
        }

        return vouchers;
    }

        /**
     * Returns the property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public String getVoucherProperty(String fiscalCode, int voucherId, String property) {
        String serverResponse = executeDBCommand(String.format("GET %s.voucher%d.%s", fiscalCode, voucherId, property));
        if (serverResponse.equals("OK ")) {
            return null;            
        }
        return serverResponse.split(" ")[1];
    }

    public void addUser(User user) {
        executeDBCommand(String.format("ADDL fiscalCodes %s", user.getFiscalCode()));
        executeDBCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "name", user.getName()));
        executeDBCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "surname", user.getSurname()));
        executeDBCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "email", user.getEmail()));
        executeDBCommand(String.format("SET %s.%s %f", user.getFiscalCode(), "balance", user.getBalance()));
    }

    /**
     * Saves the user data in the DB if the user already exists and then return true, else return 
     * False
     * 
     * @param user
     * @return boolean
     */
    public void modifyUser(User user) {
        setUserProperty(user.getFiscalCode(), "name", user.getName());
        setUserProperty(user.getFiscalCode(), "surname", user.getSurname());
        setUserProperty(user.getFiscalCode(), "email", user.getEmail());
        setUserProperty(user.getFiscalCode(), "balance", ""+user.getBalance());
    }

    /**
     * Sets the given property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public void setUserProperty(String fiscalCode, String property, String value) {
        executeDBCommand(String.format("SET %s.%s %s", fiscalCode, property, value));
    }

    public void addVoucherToUser(Voucher voucher, User user) {
        // First I add the new voucher ID to the voucherIds list of the user
        executeDBCommand(String.format("ADDL %s.vouchersIds %d", user.getFiscalCode(), voucher.getId()));

        // Then I save the voucher data in the DB
        executeDBCommand(String.format("SET %s.voucher%d.%s %s", user.getFiscalCode(), voucher.getId(), "type", voucher.getType()));
        executeDBCommand(String.format("SET %s.voucher%d.%s %f", user.getFiscalCode(), voucher.getId(), "value", voucher.getValue()));
        executeDBCommand(String.format("SET %s.voucher%d.%s %s", user.getFiscalCode(), voucher.getId(), "consumed", voucher.isConsumed()));
        executeDBCommand(String.format("SET %s.voucher%d.%s %s", user.getFiscalCode(), voucher.getId(), "createdDateTime", voucher.getCreatedDateTime()));
    }

    public void modifyUserVoucher(Voucher voucher, User user) {
        String fiscalCode = user.getFiscalCode();
        int voucherId = voucher.getId();
        executeDBCommand(String.format("SET %s.voucher%d.type %s", fiscalCode, voucherId, voucher.getType()));
        executeDBCommand(String.format("SET %s.voucher%d.value %f", fiscalCode, voucherId, voucher.getValue()));
        executeDBCommand(String.format("SET %s.voucher%d.consumed %b", fiscalCode, voucherId, voucher.isConsumed()));
        executeDBCommand(String.format("SET %s.voucher%d.consumedDateTime %s", fiscalCode, voucherId, voucher.getConsumedDateTime()));
    }

    public void deleteUserVoucher(Voucher voucher, User user) {
        String fiscalCode = user.getFiscalCode();
        int voucherId = voucher.getId();

        String deleteIdCommand = String.format("REMOVEL %s.vouchersIds %d", fiscalCode, voucherId);
        String deleteTypeCommand = String.format("CLEAR %s.voucher%d.type", fiscalCode, voucherId);
        String deleteValueCommand = String.format("CLEAR %s.voucher%d.value", fiscalCode, voucherId);
        String deleteConsumedCommand = String.format("CLEAR %s.voucher%d.consumed", fiscalCode, voucherId);
        String deleteCreatedDateTimeCommand = String.format("CLEAR %s.voucher%d.createdDateTime", fiscalCode, voucherId);
        String deleteConsumedDateTimeCommand = String.format("CLEAR %s.voucher%d.consumedDateTime", fiscalCode, voucherId);

        executeDBCommand(deleteIdCommand);
        executeDBCommand(deleteTypeCommand);
        executeDBCommand(deleteValueCommand);
        executeDBCommand(deleteConsumedCommand);
        executeDBCommand(deleteCreatedDateTimeCommand);
        executeDBCommand(deleteConsumedDateTimeCommand);
    }

    /**
     * Executes a command to the DB and returns its response
     * 
     * @param command
     * @return
     */
    public String executeDBCommand(String command) {
        String response = null;
        
        try (Socket socket = new Socket(address, port)) {
            PrintWriter socketOutputStream = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader socketInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socketOutputStream.println(command);

            String temp;
            while ((temp = socketInputStream.readLine()) != null) {
                response = temp;
                // System.out.println(String.format("[DEBUG] Server response to [%s]: %s", command, response));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return response;
    }
}
