package co.edu.uco.parameterservices.catalog.parameter.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;
import co.edu.uco.parameterservices.catalog.parameter.service.ParameterService;

@RestController
@RequestMapping("/parameters/api/v1/parameters")
public class ParameterController {

    private final ParameterService service;

    public ParameterController(ParameterService service) {
        this.service = service;
    }

    @GetMapping("/{key}")
    public ResponseEntity<Parameter> getParameter(@PathVariable String key) {
        var value = service.findByKey(key);
        return new ResponseEntity<>(value, (value == null) ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<Map<String, Parameter>> getAllParameters() {
        var all = service.findAll();
        return new ResponseEntity<>(all, HttpStatus.OK);
    }
}
