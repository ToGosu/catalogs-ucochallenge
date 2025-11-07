package co.edu.uco.parameterservices.catalog.parameter.service.impl;

import java.util.Map;
import org.springframework.stereotype.Service;

import co.edu.uco.parameterservices.catalog.parameter.ParameterCatalog;
import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;
import co.edu.uco.parameterservices.catalog.parameter.service.ParameterService;

@Service
public class ParameterServiceImpl implements ParameterService {

    private final ParameterCatalog catalog;

    public ParameterServiceImpl(ParameterCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Parameter findByKey(String key) {
        return catalog.getParameter(key);
    }

    @Override
    public Map<String, Parameter> findAll() {
        return catalog.getAllParameters();
    }
}
