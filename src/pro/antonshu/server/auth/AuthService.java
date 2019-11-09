package pro.antonshu.server.auth;

import pro.antonshu.server.User;

public interface AuthService {

    boolean authUser(User user);
}
