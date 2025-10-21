package tetoandeggens.seeyouagainbatch.common.reader;

import java.util.function.Function;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManagerFactory;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetOptions;

public class QuerydslNoOffsetPagingItemReaderBuilder<T> {
	private QuerydslNoOffsetPagingItemReaderBuilder() {
	}

	public static <T> EntityManagerFactoryStep<T> builder() {
		return new Steps<>();
	}

	public interface EntityManagerFactoryStep<T> {
		PageSizeStep<T> entityManagerFactory(EntityManagerFactory emf);
	}

	public interface PageSizeStep<T> {
		OptionsStep<T> pageSize(int pageSize);
	}

	public interface OptionsStep<T> {
		QueryFunctionStep<T> options(QuerydslNoOffsetOptions<T> options);
	}

	public interface QueryFunctionStep<T> {
		BuildStep<T> queryFunction(Function<JPAQueryFactory, JPAQuery<T>> queryFunction);
	}

	public interface BuildStep<T> {
		BuildStep<T> idSelectQuery(Function<JPAQueryFactory, JPAQuery<T>> idSelectQuery);

		QuerydslNoOffsetPagingItemReader<T> build();
	}

	private static class Steps<T> implements
		EntityManagerFactoryStep<T>, PageSizeStep<T>, OptionsStep<T>, QueryFunctionStep<T>, BuildStep<T> {
		private EntityManagerFactory entityManagerFactory;
		private int pageSize;
		private QuerydslNoOffsetOptions<T> options;
		private Function<JPAQueryFactory, JPAQuery<T>> queryFunction;
		private Function<JPAQueryFactory, JPAQuery<T>> idSelectQuery;

		@Override
		public PageSizeStep<T> entityManagerFactory(EntityManagerFactory emf) {
			this.entityManagerFactory = emf;
			return this;
		}

		@Override
		public OptionsStep<T> pageSize(int pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		@Override
		public QueryFunctionStep<T> options(QuerydslNoOffsetOptions<T> options) {
			this.options = options;
			return this;
		}

		@Override
		public BuildStep<T> queryFunction(Function<JPAQueryFactory, JPAQuery<T>> queryFunction) {
			this.queryFunction = queryFunction;
			return this;
		}

		@Override
		public BuildStep<T> idSelectQuery(Function<JPAQueryFactory, JPAQuery<T>> idSelectQuery) {
			this.idSelectQuery = idSelectQuery;
			return this;
		}

		@Override
		public QuerydslNoOffsetPagingItemReader<T> build() {
			return new QuerydslNoOffsetPagingItemReader<>(
				entityManagerFactory,
				pageSize,
				options,
				queryFunction,
				idSelectQuery
			);
		}
	}
}