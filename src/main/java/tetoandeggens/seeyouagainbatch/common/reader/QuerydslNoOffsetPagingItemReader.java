package tetoandeggens.seeyouagainbatch.common.reader;

import java.util.function.Function;

import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetOptions;

public class QuerydslNoOffsetPagingItemReader<T> extends QuerydslPagingItemReader<T> {
	private QuerydslNoOffsetOptions<T> options;
	private Function<JPAQueryFactory, JPAQuery<T>> idSelectQuery;

	private QuerydslNoOffsetPagingItemReader() {
		super();
		setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
	}

	public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory,
		int pageSize,
		QuerydslNoOffsetOptions<T> options,
		Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
		super(entityManagerFactory, pageSize, queryFunction);
		setName(ClassUtils.getShortName(QuerydslNoOffsetPagingItemReader.class));
		this.options = options;
	}

	public QuerydslNoOffsetPagingItemReader(EntityManagerFactory entityManagerFactory,
		int pageSize,
		QuerydslNoOffsetOptions<T> options,
		Function<JPAQueryFactory, JPAQuery<T>> queryFunction,
		Function<JPAQueryFactory, JPAQuery<T>> idSelectQuery) {
		this(entityManagerFactory, pageSize, options, queryFunction);
		this.idSelectQuery = idSelectQuery;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doReadPage() {

		EntityTransaction tx = getTxOrNull();

		JPQLQuery<T> query = createQuery().limit(getPageSize());

		initResults();

		fetchQuery(query, tx);

		resetCurrentIdIfNotLastPage();
	}

	@Override
	protected JPAQuery<T> createQuery() {
		JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
		JPAQuery<T> query = queryFunction.apply(queryFactory);
		options.initKeys((idSelectQuery != null) ? idSelectQuery.apply(queryFactory) : query,
			getPage());

		return options.createQuery(query, getPage());
	}

	private void resetCurrentIdIfNotLastPage() {
		if (isNotEmptyResults()) {
			options.resetCurrentId(getLastItem());
		}
	}

	private boolean isNotEmptyResults() {
		return !CollectionUtils.isEmpty(results) && results.get(0) != null;
	}

	private T getLastItem() {
		return results.get(results.size() - 1);
	}
}