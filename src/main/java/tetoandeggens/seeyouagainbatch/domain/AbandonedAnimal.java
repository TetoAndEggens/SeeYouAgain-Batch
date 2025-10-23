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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ABANDONED_ANIMAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbandonedAnimal extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "abandoned_animal_id")
	private Long id;

	@Column(name = "desertion_no", unique = true)
	private String desertionNo;

	@Column(name = "happen_date")
	private LocalDate happenDate;

	@Column(name = "happen_place")
	private String happenPlace;

	@Enumerated(EnumType.STRING)
	@Column(name = "species")
	private Species species;

	@Column(name = "color")
	private String color;

	@Column(name = "birth")
	private String birth;

	@Column(name = "weight")
	private String weight;

	@Column(name = "notice_no", unique = true)
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "center_location_id")
	private CenterLocation centerLocation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "breed_type_id")
	private BreedType breedType;
}
