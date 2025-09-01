package com.middleware.backend.users.tokens.service;

import com.middleware.backend.users.tokens.dto.TokenDto;
import com.middleware.backend.users.tokens.mapper.TokenMapper;
import com.middleware.backend.users.tokens.model.Token;
import com.middleware.backend.users.tokens.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TokenService {
    private final TokenRepository repo;

    public void save (Token token){
        Optional<List<Token>> exists = repo.findAllByUser_Id(token.getUser().getId());
        if(exists.isPresent()){
            repo.removeByUser_Id(token.getUser().getId());
        }
        repo.save(token);
    }

    public ResponseEntity<?> logout(String email) {
        System.out.println("mmksmwkmwskmdeknewne");
        System.out.println(email);
        Optional<Token> tok = repo.findByUser_Email(email);
        tok.get().setValid(false);
        return ResponseEntity.ok(repo.save(tok.get()));
    }
}
