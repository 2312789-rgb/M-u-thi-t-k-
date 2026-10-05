package vn.macstudio.macstudio.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.macstudio.macstudio.repository.AppUserRepository;

@Service
public class StoreUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;
    public StoreUserDetailsService(AppUserRepository users) { this.users = users; }
    @Override public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));
    }
}