package it.unimib.sd2025.db;

import java.util.List;
import java.util.Map;

import it.unimib.sd2025.models.User;
import it.unimib.sd2025.models.Voucher;

public interface IUserDao {
    Map<String, User> getUsers();
    public void addUser(User user);
}