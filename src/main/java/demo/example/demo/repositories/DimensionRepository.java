package demo.example.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.example.demo.entity.Dimension;

public interface DimensionRepository extends JpaRepository<Dimension, Integer> {}