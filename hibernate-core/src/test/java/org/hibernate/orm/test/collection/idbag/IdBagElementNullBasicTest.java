/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.idbag;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
public class IdBagElementNullBasicTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistNullValue() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( null );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void addNullValue() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					e.aCollection.add( null );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ) );
					e.aCollection.set( 0, null );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNullWithExtraValue() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					e.aCollection.add( "ghi" );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.aCollection.size() );
					assertEquals( 2, getCollectionElementRows( e.id ) );
					if ( "def".equals( e.aCollection.get( 0 ) ) ) {
						e.aCollection.set( 0, null );
					}
					else {
						e.aCollection.set( 1, null );
					}
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( e.id ) );
					assertEquals( "ghi", e.aCollection.get( 0 ) );
					session.delete( e );
				}
		);
	}

	private int getCollectionElementRows(int id) {
		return inTransaction(
				session -> {
					return session.doReturningWork(
							// todo (6.0) : use native query when native queries will be implemented
							work -> {
								PreparedStatement statement = null;
								ResultSet resultSet = null;

								try {
									statement = work.prepareStatement(
											"SELECT count(aCollection) as numberOfRows FROM AnEntity_aCollection where AnEntity_id = " + id );
									statement.execute();
									resultSet = statement.getResultSet();
									if ( resultSet.next() ) {
										return resultSet.getInt( "numberOfRows" );
									}
									return 0;
								}
								finally {
									if ( resultSet != null && !resultSet.isClosed() ) {
										resultSet.close();
									}
									if ( statement != null && !statement.isClosed() ) {
										statement.close();
									}
								}
							}
					);
				}
		);
	}

	@Entity
	@Table(name = "AnEntity")
	@GenericGenerator(name = "increment", strategy = "increment")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = { @JoinColumn(name = "AnEntity_id") })
		@CollectionId(
				columns = { @Column },
				type = @Type(type = "long"),
				generator = "increment"
		)
		private List<String> aCollection = new ArrayList<String>();
	}
}