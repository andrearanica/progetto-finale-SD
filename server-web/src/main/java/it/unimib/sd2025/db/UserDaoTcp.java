package it.unimib.sd2025.db;

import it.unimib.sd2025.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
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
        System.out.println("[DEBUG] Connected to DB server");

        List<String> fiscalCodes = getFiscalCodes();

        Map<String, User> users = new HashMap<String, User>();

        for (String fiscalCode : fiscalCodes) {
            // FIXME sucks
            if (fiscalCode.equals("OK") || fiscalCode.equals("")) {
                continue;
            }

            // FIXME remove this try-catch
            String userName = getUserProperty(fiscalCode, "name");
            String userSurname = getUserProperty(fiscalCode, "surname");
            String userEmail = getUserProperty(fiscalCode, "email");
            String userBalanceRaw = getUserProperty(fiscalCode, "balance");
            List<Voucher> userVouchers = getUserVouchers(fiscalCode);

            System.out.println(String.format("[DEBUG] Fetched user with\nname: %s\nsurname: %s\nemail: %s\nbalance: %s\nvouchers:" + userVouchers, 
                                            userName, userSurname, userEmail, userBalanceRaw));

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
        String rawFiscalCodes = executeCommand("GETL fiscalCodes");
        
        System.out.printf("[DEBUG] Fiscal codes: %s\n", rawFiscalCodes);
        
        String[] fiscalCodes = rawFiscalCodes.split(" ");

        return new ArrayList<>(Arrays.asList(fiscalCodes));
    }

    /**
     * Returns the property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public String getUserProperty(String fiscalCode, String property) {
        String serverResponse = executeCommand(String.format("GET %s.%s", fiscalCode, property));
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

        String vouchersIdsRaw = executeCommand(String.format("GETL %s.vouchersIds", fiscalCode));
        String[] vouchersIds = vouchersIdsRaw.split(" ");

        for (String voucherIdRaw : vouchersIds) {
            System.out.println(String.format("[DEBUG] Voucher ID: %s", voucherIdRaw));

            // FIXME sucks
            if (voucherIdRaw.equals("OK")) {
                continue;
            }

            int voucherId = Integer.parseInt(voucherIdRaw);

            String voucherValueRaw = getVoucherProperty(fiscalCode, voucherId, "value");
            String voucherConsumedRaw = getVoucherProperty(fiscalCode, voucherId, "consumed");
            String voucherType = getVoucherProperty(fiscalCode, voucherId, "type");
            // TODO get also created and consumed dates

            if (voucherValueRaw == null || voucherConsumedRaw == null || voucherType == null) {
                System.out.println(String.format("[DEBUG] DB inconsistency: missing data for voucher %d of the user %s", 
                                                 voucherId, fiscalCode));
                continue;
            }

            Voucher voucher = new Voucher();
            voucher.setId(voucherId);
            voucher.setValue(Float.parseFloat(voucherValueRaw.replace(",", ".")));
            voucher.setConsumed(Boolean.parseBoolean(voucherConsumedRaw));
            voucher.setType(voucherType);

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
        String serverResponse = executeCommand(String.format("GET %s.voucher%d.%s", fiscalCode, voucherId, property));
        if (serverResponse.equals("OK ")) {
            return null;            
        }
        return serverResponse.split(" ")[1];
    }

    public void addUser(User user) {
        List<String> fiscalCodes = getFiscalCodes();

        try {
            fiscalCodes.add(user.getFiscalCode());
            executeCommand("SETL fiscalCodes " + String.join(" ", fiscalCodes));
            
            executeCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "name", user.getName()));
            executeCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "surname", user.getSurname()));
            executeCommand(String.format("SET %s.%s %s", user.getFiscalCode(), "email", user.getEmail()));
            executeCommand(String.format("SET %s.%s %f", user.getFiscalCode(), "balance", user.getBalance()));
        } catch (Exception e) {
            System.out.println("[DEBUG] Exception: " + e.getClass() + "(" + e.getMessage() + ")");
        }
    }

    /**
     * Saves the user data in the DB if the user already exists and then return true, else return 
     * False
     * 
     * @param user
     * @return boolean
     */
    public boolean modifyUser(User user) {
        List<String> fiscalCodes = getFiscalCodes();

        if (fiscalCodes.contains(user.getFiscalCode())) {
            setUserProperty(user.getFiscalCode(), "name", user.getName());
            setUserProperty(user.getFiscalCode(), "surname", user.getSurname());
            setUserProperty(user.getFiscalCode(), "email", user.getEmail());
            
            return true;
        } else {
            System.out.println("[DEBUG] Cannot find user with fiscal code " + user.getFiscalCode());
            return false;
        }
    }

    /**
     * Sets the given property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public void setUserProperty(String fiscalCode, String property, String value) {
        executeCommand(String.format("SET %s.%s %s", fiscalCode, property, value));
    }

    public boolean addVoucherToUser(Voucher voucher, User user) {
        List<String> fiscalCodes = getFiscalCodes();
        // TODO check that the vocher ID isn't already used

        if (fiscalCodes.contains(user.getFiscalCode())) {
            // First I add the new voucher ID to the voucherIds list of the user
            
            List<String> vouchersIds = new ArrayList<String>();

            for (Voucher v : user.getVouchers()) {
                vouchersIds.add(String.valueOf(v.getId()));
            }

            vouchersIds.add(String.valueOf(voucher.getId()));

            executeCommand(String.format("SETL %s.vouchersIds %s", user.getFiscalCode(), String.join(" ", vouchersIds)));

            // Then I save the voucher data in the DB
            executeCommand(String.format("SET %s.voucher%d.%s %s", user.getFiscalCode(), voucher.getId(), "type", voucher.getType()));
            executeCommand(String.format("SET %s.voucher%d.%s %f", user.getFiscalCode(), voucher.getId(), "value", voucher.getValue()));
            executeCommand(String.format("SET %s.voucher%d.%s %s", user.getFiscalCode(), voucher.getId(), "consumed", voucher.isConsumed()));

            return true;
        } else {
            System.out.println("[DEBUG] Cannot find user with fiscal code " + user.getFiscalCode());
            return false;
        }
    }

    /**
     * Executes a command to the DB and returns its response
     * 
     * @param command
     * @return
     */
    public String executeCommand(String command) {
        String response = null;
        
        try (Socket socket = new Socket(address, port)) {
            PrintWriter socketOutputStream = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader socketInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socketOutputStream.println(command);

            String temp;
            while ((temp = socketInputStream.readLine()) != null) {
                response = temp;
                System.out.println(String.format("[DEBUG] Server response to [%s]: %s", command, response));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return response;
    }
}
