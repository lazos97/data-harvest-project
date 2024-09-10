package com.dataharvest.service;

import com.dataharvest.model.Client;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Optional;

@Service
public class ClientService {

    private final JdbcTemplate jdbcTemplate;

    public ClientService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final String SELECT_CLIENT_BY_CLIENT_NUMBER = "SELECT * FROM client WHERE uic = ?";

    private static final RowMapper<Client> CLIENT_ROW_MAPPER = (rs, rowNum) -> {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setClientNumber(rs.getString("client_number"));
        client.setCompanyName(rs.getString("company_name"));
        client.setRegisteredOffice(rs.getString("registered_office"));
        client.setRepresentative(rs.getString("representative"));
        client.setUic(rs.getString("uic"));
        return client;
    };

    @SuppressWarnings("deprecation")
    public Optional<Client> findClientByUIC(String uic) {
        return jdbcTemplate.query(SELECT_CLIENT_BY_CLIENT_NUMBER, new Object[]{uic}, CLIENT_ROW_MAPPER).stream().findFirst();
    }

    public void insertClient(Client client) {
        String insertSql = "INSERT INTO client (client_number, company_name, registered_office, representative, uic) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertSql,
            client.getClientNumber(),
            client.getCompanyName(),
            truncateToLength(client.getRegisteredOffice(), 1000), 
            client.getRepresentative(),
            client.getUic());
    }
    
    private String truncateToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public Object findClientByCompanyName(String company) {
        throw new UnsupportedOperationException("Unimplemented method 'findClientByCompanyName'");
    }
}