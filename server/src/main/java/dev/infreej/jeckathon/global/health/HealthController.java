package dev.infreej.jeckathon.global.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 배포 후 상태 확인용이다. GitHub Actions가 이 경로가 200을 반환하는지 확인한다.
@RestController
public class HealthController {

	@GetMapping("/api/health")
	public ResponseEntity<Void> health() {
		return ResponseEntity.ok().build();
	}
}
