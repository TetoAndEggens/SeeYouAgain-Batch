package tetoandeggens.seeyouagainbatch.job.keywordnotification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordNotificationDto {

	private Long notificationKeywordId;
	private String keyword;
	private Long animalCount;
	private KeywordType keywordType;
	private KeywordCategoryType keywordCategoryType;
}