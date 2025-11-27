package kr.gitrank.api.global.jpa.common

import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    val id: UUID = UlidCreator.getMonotonicUlid().toUuid()

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    var deletedAt: LocalDateTime? = null
        protected set

    val isDeleted: Boolean
        get() = deletedAt != null

    fun delete() {
        deletedAt = LocalDateTime.now()
    }

    fun activate() {
        deletedAt = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
