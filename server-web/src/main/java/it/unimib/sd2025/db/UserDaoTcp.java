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

    private final char SPACE_DELIMITER = '~';
    
    public UserDaoTcp(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public Map<String, User> getUsers() {
        List<String> fiscalCodes = getFiscalCodes();

        Map<String, User> users = new HashMap<String, User>();

        for (String fiscalCode : fiscalCodes) {
            String userName = getUserPropertyFromDB(fiscalCode, "name");
            String userSurname = getUserPropertyFromDB(fiscalCode, "surname");
            String userEmail = getUserPropertyFromDB(fiscalCode, "email");
            String userBalanceRaw = getUserPropertyFromDB(fiscalCode, "balance");
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
    public String getUserPropertyFromDB(String fiscalCode, String property) {
        String serverResponseWithOk = executeDBCommand(String.format("GET %s.%s", fiscalCode, property));
        if (serverResponseWithOk.equals("OK ")) {
            return null;
        }
        String serverResponseWithoutSpaces = serverResponseWithOk.split(" ")[1];

        // Before returning the response, I replace every space delimiter with a space character
        String serverResponse = serverResponseWithoutSpaces.replace(SPACE_DELIMITER, ' ');
        return serverResponse;
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

            String voucherValueRaw = getVoucherPropertyFromDB(fiscalCode, voucherId, "value");
            String voucherConsumedRaw = getVoucherPropertyFromDB(fiscalCode, voucherId, "consumed");
            String voucherType = getVoucherPropertyFromDB(fiscalCode, voucherId, "type");
            String voucherCreatedDateTime = getVoucherPropertyFromDB(fiscalCode, voucherId, "createdDateTime");
            String voucherConsumedDateTime = getVoucherPropertyFromDB(fiscalCode, voucherId, "consumedDateTime");

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
    public String getVoucherPropertyFromDB(String fiscalCode, int voucherId, String property) {
        String serverResponse = executeDBCommand(String.format("GET %s.voucher%d.%s", fiscalCode, voucherId, property));
        if (serverResponse.equals("OK ")) {
            return null;            
        }

        // Before returning the property, I replace each special character with the space
        String serverResponseWithoutSpaces = serverResponse.split(" ")[1];
        String serverResponseWithSpaces = serverResponseWithoutSpaces.replace(SPACE_DELIMITER, ' ');
        return serverResponseWithSpaces;
    }

    /**
     * Sets the voucher property with the given data in the DB
     * 
     * @param fiscalCode
     * @param voucherId
     * @param property
     */
    public void saveVoucherPropertyInDB(String fiscalCode, int voucherId, String property, String value) {
        // Before running the command, I convert each space with the special character
        String valueWithoutSpaces = value.replace(' ', SPACE_DELIMITER);

        executeDBCommand(String.format("SET %s.voucher%d.%s %s", fiscalCode, voucherId, property, valueWithoutSpaces));
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
        saveUserPropertyInDB(user.getFiscalCode(), "name", user.getName());
        saveUserPropertyInDB(user.getFiscalCode(), "surname", user.getSurname());
        saveUserPropertyInDB(user.getFiscalCode(), "email", user.getEmail());
        saveUserPropertyInDB(user.getFiscalCode(), "balance", ""+user.getBalance());
    }

    /**
     * Sets the given property of the user that has the given fiscalCode, saved in the DB
     * 
     * @param fiscalCode
     * @param property, the key of the value in the DB
     * @return
     */
    public void saveUserPropertyInDB(String fiscalCode, String property, String value) {
        // Before running the command, I have to replace spaces with a special character because
        if (value.contains(" ")) {
            value.replace(' ', SPACE_DELIMITER);
        }
        executeDBCommand(String.format("SET %s.%s %s", fiscalCode, property, value));
    }

    public void addVoucherToUser(Voucher voucher, User user) {
        // First I add the new voucher ID to the voucherIds list of the user
        executeDBCommand(String.format("ADDL %s.vouchersIds %d", user.getFiscalCode(), voucher.getId()));

        // Then I save the voucher data in the DB
        String fiscalCode = user.getFiscalCode();
        int voucherId = voucher.getId();
        saveVoucherPropertyInDB(fiscalCode, voucherId, "type", voucher.getType());
        saveVoucherPropertyInDB(fiscalCode, voucherId, "value", String.valueOf(voucher.getValue()));
        saveVoucherPropertyInDB(fiscalCode, voucherId, "consumed", Boolean.toString(voucher.isConsumed()));
        saveVoucherPropertyInDB(fiscalCode, voucherId, "createdDateTime", voucher.getCreatedDateTime());
    }

    public void modifyUserVoucher(Voucher voucher, User user) {
        String fiscalCode = user.getFiscalCode();
        int voucherId = voucher.getId();
        saveVoucherPropertyInDB(fiscalCode, voucherId, "type", voucher.getType());
        saveVoucherPropertyInDB(fiscalCode, voucherId, "value", String.valueOf(voucher.getValue()));
        saveVoucherPropertyInDB(fiscalCode, voucherId, "consumed", Boolean.toString(voucher.isConsumed()));
        saveVoucherPropertyInDB(fiscalCode, voucherId, "createdDateTime", voucher.getCreatedDateTime());
        if (voucher.getConsumedDateTime() != null) {
            saveVoucherPropertyInDB(fiscalCode, voucherId, "consumedDateTime", voucher.getConsumedDateTime());
        }
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
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return response;
    }
}
