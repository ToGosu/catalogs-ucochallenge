package co.edu.uco.parameterservices.catalog.parameter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;

public interface ParameterRepository extends JpaRepository<Parameter, String> {

}
