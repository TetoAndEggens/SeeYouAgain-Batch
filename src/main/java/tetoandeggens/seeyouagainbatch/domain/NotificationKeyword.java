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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "NOTIFICATION_KEYWORD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationKeyword extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_keyword_id")
	private Long id;

	@Column(name = "keyword")
	private String keyword;

	@Enumerated(EnumType.STRING)
	@Column(name = "keyword_type")
	private KeywordType keywordType;

	@Enumerated(EnumType.STRING)
	@Column(name = "keyword_category_type")
	private KeywordCategoryType keywordCategoryType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;
}
