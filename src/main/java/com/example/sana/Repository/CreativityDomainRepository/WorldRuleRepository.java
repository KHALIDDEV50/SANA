package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.WorldRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorldRuleRepository extends JpaRepository<WorldRule, Integer> {

    WorldRule findWorldRuleById(Integer id);

}

