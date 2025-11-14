package tetoandeggens.seeyouagainbatch.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ANIMAL_PROFILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalProfile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_profile_id")
	private Long id;

	@Column(name = "profile")
	private String profile;

	@Column(name = "happen_date")
	private LocalDate happenDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "animal_id")
	private Animal animal;

	@Builder
	public AnimalProfile(String profile, LocalDate happenDate, Animal animal) {
		this.profile = profile;
		this.happenDate = happenDate;
		this.animal = animal;
	}

	public void clearProfile() {
		this.profile = null;
	}
}