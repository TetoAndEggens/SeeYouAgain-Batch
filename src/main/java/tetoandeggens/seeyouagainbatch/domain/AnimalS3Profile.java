package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "ANIMAL_S3_PROFILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalS3Profile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_s3_profile_id")
	private Long id;

	@Column(name = "profile", unique = true)
	private String profile;

	@Enumerated(EnumType.STRING)
	@Column(name = "image_type")
	private ImageType imageType = ImageType.WEBP;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "animal_id")
	private Animal animal;

	@Builder
	public AnimalS3Profile(String profile, Animal animal) {
		this.profile = profile;
		this.animal = animal;
	}
}