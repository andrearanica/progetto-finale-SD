package it.unimib.sd2025.db;

import java.util.List;

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;

public interface IUserDao {
    public List<User> getAllUsers();
    public void addUser(User user);
    public void modifyUser(User user);
    public void addVoucherToUser(Voucher voucher, User user);
    public void modifyUserVoucher(Voucher voucher, User user);
    public void deleteUserVoucher(Voucher voucher, User user);
}