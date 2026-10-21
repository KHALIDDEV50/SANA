package com.example.sana.Repository;

import com.example.sana.Model.ToolRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolRunRepository extends JpaRepository<ToolRun, Integer> {

    ToolRun findToolRunById(Integer id);
}
