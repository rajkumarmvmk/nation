package com.example.nation.repository;

import com.example.nation.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity,String> {
    @Query(value = "SELECT * FROM country WHERE country_code=:countryCode",nativeQuery = true )
    Optional<CountryEntity> findByCountryCode(String countryCode);

    @Modifying
    @Query("delete from CountryEntity t where t.id = ?1")
    void deleteAllById(String id);

}
