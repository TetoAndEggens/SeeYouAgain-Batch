package tetoandeggens.seeyouagainbatch.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "ANIMAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Animal extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "animal_type", nullable = false)
	private AnimalType animalType = AnimalType.ABANDONED;

	@Column(name = "desertion_no")
	private String desertionNo;

	@Column(name = "happen_date")
	private LocalDate happenDate;

	@Column(name = "happen_place")
	private String happenPlace;

	@Column(name = "city")
	private String city;

	@Column(name = "town")
	private String town;

	@Enumerated(EnumType.STRING)
	@Column(name = "species")
	private Species species;

	@Column(name = "color")
	private String color;

	@Column(name = "birth")
	private String birth;

	@Column(name = "weight")
	private String weight;

	@Column(name = "notice_no")
	private String noticeNo;

	@Column(name = "notice_start_date")
	private String noticeStartDate;

	@Column(name = "notice_end_date")
	private String noticeEndDate;

	@Column(name = "process_state")
	private String processState;

	@Enumerated(EnumType.STRING)
	@Column(name = "sex")
	private Sex sex;

	@Enumerated(EnumType.STRING)
	@Column(name = "neutered_state")
	private NeuteredState neuteredState;

	@Column(name = "special_mark")
	private String specialMark;

	@Column(name = "center_phone")
	private String centerPhone;

	@Column(name = "final_updated_at")
	private LocalDateTime finalUpdatedAt;

	@Column(name = "is_deleted")
	private Boolean isDeleted = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "animal_location_id")
	private AnimalLocation animalLocation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "breed_type_id")
	private BreedType breedType;

	public Animal(Long id) {
		this.id = id;
	}

	@Builder
	public Animal(String city, String town, String birth, BreedType breedType, AnimalLocation animalLocation,
		String centerPhone,
		String color, String desertionNo, LocalDateTime finalUpdatedAt, LocalDate happenDate, String happenPlace,
		NeuteredState neuteredState, String noticeEndDate, String noticeNo, String noticeStartDate, String processState,
		Sex sex, String specialMark, Species species, String weight) {
		this.animalType = AnimalType.ABANDONED;
		this.city = city;
		this.town = town;
		this.birth = birth;
		this.breedType = breedType;
		this.animalLocation = animalLocation;
		this.centerPhone = centerPhone;
		this.color = color;
		this.desertionNo = desertionNo;
		this.finalUpdatedAt = finalUpdatedAt;
		this.happenDate = happenDate;
		this.happenPlace = happenPlace;
		this.neuteredState = neuteredState;
		this.noticeEndDate = noticeEndDate;
		this.noticeNo = noticeNo;
		this.noticeStartDate = noticeStartDate;
		this.processState = processState;
		this.sex = sex;
		this.specialMark = specialMark;
		this.species = species;
		this.weight = weight;
		this.isDeleted = false;
	}
}