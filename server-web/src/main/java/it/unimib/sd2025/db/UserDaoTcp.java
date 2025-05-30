package it.unimib.sd2025.db;

import it.unimib.sd2025.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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

        String[] fiscalCodes = getFiscalCodes();

        Map<String, User> users = new HashMap<String, User>();

        for (String fiscalCode : fiscalCodes) {
            // FIXME sucks
            if (fiscalCode.equals("OK") || fiscalCode.equals("")) {
                continue;
            }

            // FIXME remove this try-catch
            try {
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
                user.setBalance(Float.parseFloat(userBalanceRaw));
                user.setVouchers(userVouchers);

                users.put(fiscalCode, user);
            } catch (Exception e) {
                System.out.println(String.format("[DEBUG] Data of user %s is missing in the DB", fiscalCode));
            }
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
    public String[] getFiscalCodes() {
        String rawFiscalCodes = executeCommand("GETL fiscalCodes");
        
        System.out.printf("[DEBUG] Fiscal codes: %s\n", rawFiscalCodes);
        
        String[] fiscalCodes = rawFiscalCodes.split(" ");

        return fiscalCodes;
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
        if (serverResponse.equals("OK")) {
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

        String vouchersIdsRaw = executeCommand(String.format("GET %s.vouchersIds", fiscalCode));
        String[] vouchersIds = vouchersIdsRaw.split(" ");

        for (String voucherIdRaw : vouchersIds) {
            System.out.println(String.format("[DEBUG] Voucher ID: %s", voucherIdRaw));

            // FIXME sucks
            if (voucherIdRaw.equals("OK")) {
                continue;
            }

            int voucherId = Integer.parseInt(voucherIdRaw);

            String voucherValueRaw = getVoucherProperty(fiscalCode, voucherId, "value");
            String voucherConsumedRaw = getVoucherProperty(fiscalCode, voucherId, "value");
            String voucherType = getVoucherProperty(fiscalCode, voucherId, "type");
            // TODO get also created and consumed dates

            Voucher voucher = new Voucher();
            voucher.setId(voucherId);
            voucher.setValue(Float.parseFloat(voucherValueRaw));
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
        return executeCommand(String.format("GET %s.voucher%i.%s", fiscalCode, voucherId, property));
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
