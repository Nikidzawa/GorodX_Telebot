package ru.nikidzawa.datingapp.API;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.client.RestTemplate;

@Component
@CrossOrigin
public class ExternalApi {
    String[] apiKeysPull = {"a9e37ef6-bfed-43a3-81eb-1f8cbd52bbc1", "6fcdd42f-151b-4068-b587-ffaec9a48512", "69b2e791-2b45-4268-841f-2a98c25e62cc"};
    int currentKey = 0;

    public String getCoordinates(String geoObject) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;

        String url = "https://geocode-maps.yandex.ru/1.x?apikey=" + apiKeysPull[currentKey] + "&format=json&geocode=" + geoObject;
        response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            currentKey++;
            if (currentKey >= apiKeysPull.length) {
                currentKey = 0;
            }
            getCoordinates(geoObject);
        }

        return response.getBody();
    }

    public String getCityName(double latitude, double longitude) {
        String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + latitude + "&lon=" + longitude + "&addressdetails=1";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;
        response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );
        return response.getBody();
    }
}
