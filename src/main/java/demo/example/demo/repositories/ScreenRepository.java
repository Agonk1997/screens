package demo.example.demo.repositories;

import demo.example.demo.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Integer> {

    @Query(value = "SELECT * FROM \"Screen\" ORDER BY id", nativeQuery = true)
    List<Screen> findAllReal();
}
