package it.unimib.sd2025.db;

import java.util.List;
import java.util.Map;

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;

public interface IUserDao {
    public Map<String, User> getUsers();
    public void addUser(User user);
    public void modifyUser(User user);
    public void addVoucherToUser(Voucher voucher, User user);
    public void modifyUserVoucher(Voucher voucher, User user);
    public void deleteUserVoucher(Voucher voucher, User user);
}