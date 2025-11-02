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
@Table(name = "ABANDONED_ANIMAL_PROFILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbandonedAnimalProfile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "abandoned_animal_profile_id")
	private Long id;

	@Column(name = "profile")
	private String profile;

	@Column(name = "happen_date")
	private LocalDate happenDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "abandoned_animal_id")
	private AbandonedAnimal abandonedAnimal;

	@Builder
	public AbandonedAnimalProfile(String profile, LocalDate happenDate, AbandonedAnimal abandonedAnimal) {
		this.profile = profile;
		this.happenDate = happenDate;
		this.abandonedAnimal = abandonedAnimal;
	}
}
