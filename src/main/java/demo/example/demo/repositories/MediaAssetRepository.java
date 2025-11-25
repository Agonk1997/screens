package demo.example.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import demo.example.demo.entity.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Integer> {}