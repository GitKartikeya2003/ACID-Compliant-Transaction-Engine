package com.banking.netBankingBackend.service.impl.security;


import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.entity.principal.UserPrincipal;
import com.banking.netBankingBackend.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email).orElseThrow(
                ()-> new UsernameNotFoundException("User with email " + email + " not found")
        );

        return new UserPrincipal(user);
    }
}
