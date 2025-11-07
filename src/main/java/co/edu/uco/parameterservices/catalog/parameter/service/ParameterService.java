package co.edu.uco.parameterservices.catalog.parameter.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;

@Service
public interface ParameterService {
    Parameter findByKey(String key);
    Map<String, Parameter> findAll();
}