package it.unimib.sd2025.db;
import it.unimib.sd2025.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDaoTcp implements IUserDao {
    private String address;
    private int port;

    private final char SPACE_DELIMITER = '~';
    
    public UserDaoTcp(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public List<User> getAllUsers() {
        List<String> fiscalCodes = getListFromDB("fiscalCodes");
        List<User> users = new ArrayList<User>();

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

            users.add(user);
        }

        return users;
    }

    public void addUser(User user) {
        String balanceRaw = String.valueOf(user.getBalance());

        addItemToListInDB("fiscalCodes", user.getFiscalCode());
        saveUserPropertyInDB(user.getFiscalCode(), "name", user.getName());
        saveUserPropertyInDB(user.getFiscalCode(), "surname", user.getSurname());
        saveUserPropertyInDB(user.getFiscalCode(), "email", user.getEmail());
        saveUserPropertyInDB(user.getFiscalCode(), "balance", balanceRaw);
    }

    public List<Voucher> getUserVouchers(String fiscalCode) {
        List<Voucher> vouchers = new ArrayList<Voucher>();

        List<String> vouchersIds = getListFromDB(String.format("%s.vouchersIds", fiscalCode));

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

    public void modifyUser(User user) {
        saveUserPropertyInDB(user.getFiscalCode(), "name", user.getName());
        saveUserPropertyInDB(user.getFiscalCode(), "surname", user.getSurname());
        saveUserPropertyInDB(user.getFiscalCode(), "email", user.getEmail());
        saveUserPropertyInDB(user.getFiscalCode(), "balance", ""+user.getBalance());
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

    private String getUserPropertyFromDB(String fiscalCode, String property) {
        String serverResponseWithOk = executeDBCommand(String.format("GET %s.%s", fiscalCode, property));
        if (serverResponseWithOk.equals("OK ")) {
            return null;
        }
        String serverResponseWithoutSpaces = serverResponseWithOk.split(" ")[1];

        // Before returning the response, I replace every space delimiter with a space character
        String serverResponse = serverResponseWithoutSpaces.replace(SPACE_DELIMITER, ' ');
        return serverResponse;
    }

    private String getVoucherPropertyFromDB(String fiscalCode, int voucherId, String property) {
        String serverResponse = executeDBCommand(String.format("GET %s.voucher%d.%s", fiscalCode, voucherId, property));
        if (serverResponse.equals("OK ")) {
            return null;            
        }

        String serverResponseWithoutSpaces = serverResponse.split(" ")[1];
        String serverResponseWithSpaces = serverResponseWithoutSpaces.replace(SPACE_DELIMITER, ' ');
        return serverResponseWithSpaces;
    }

    private void saveVoucherPropertyInDB(String fiscalCode, int voucherId, String property, String value) {
        // Before running the command, I convert each space with the special character
        String valueWithoutSpaces = value.replace(' ', SPACE_DELIMITER);

        executeDBCommand(String.format("SET %s.voucher%d.%s %s", fiscalCode, voucherId, property, valueWithoutSpaces));
    }

    private void saveUserPropertyInDB(String fiscalCode, String property, String value) {
        // Before running the command, I have to replace spaces with a special character because
        String valueWithoutSpaces = value.replace(' ', SPACE_DELIMITER);
        executeDBCommand(String.format("SET %s.%s %s", fiscalCode, property, valueWithoutSpaces));
    }

    private void addItemToListInDB(String listName, String item) {
        executeDBCommand(String.format("ADDL %s %s", listName, item));
    }

    private List<String> getListFromDB(String listName) {
        String listRaw = executeDBCommand(String.format("GETL %s", listName));
        String[] listWithOk = listRaw.split(" ");
        String[] list = Arrays.copyOfRange(listWithOk, 1, listWithOk.length);
        return Arrays.asList(list);
    }

    private String executeDBCommand(String command) {
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
