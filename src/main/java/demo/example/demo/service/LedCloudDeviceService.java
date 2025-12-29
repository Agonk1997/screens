package demo.example.demo.service;

import demo.example.demo.dto.DeviceListResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LedCloudDeviceService {

    private final LedCloudAuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    public LedCloudDeviceService(LedCloudAuthService authService) {
        this.authService = authService;
    }

    public DeviceListResponse getDevices() {
        String token = authService.getAccessToken();

        String url = "https://led-cloud.com/v1/device/list?pageNum=1&pageSize=50&recursive=true&groupId=-1";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<DeviceListResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, DeviceListResponse.class);

        return response.getBody();
    }
}
