package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(name = "login_id", unique = true)
	private String loginId;

	@Column(name = "password")
	private String password;

	@Column(name = "nick_name", nullable = false)
	private String nickName;

	@Column(name = "phone_number", nullable = false, unique = true)
	private String phoneNumber;

	@Column(name = "profile")
	private String profile;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Role role = Role.USER;

	@Column(name = "uuid", unique = true, nullable = false)
	private String uuid;

	@Column(name = "social_id_kakao", unique = true)
	private String socialIdKakao;

	@Column(name = "social_id_naver", unique = true)
	private String socialIdNaver;

	@Column(name = "social_id_google", unique = true)
	private String socialIdGoogle;

	@Column(name = "violated_count", nullable = false)
	private Long violatedCount = 0L;

	@Column(name = "is_push_enabled", nullable = false)
	private Boolean isPushEnabled = false;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;
}