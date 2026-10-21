package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, Integer> {

    CharacterProfile findCharacterProfileById(Integer id);

}

