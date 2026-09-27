# `@CacheEvict` Execution Timing

`@CacheEvict` can remove a cache entry either **before** or **after** the annotated method executes. The behavior depends on the `beforeInvocation` attribute.

## Current behavior

The current annotation is:

```java
@CacheEvict(cacheNames = "usersById", key = "#id")
public User updateRole(Long id, UpdateRoleRequest request) {
    // ...
}
```

When `beforeInvocation` is not specified, Spring uses:

```java
beforeInvocation = false
```

Therefore, the cache entry is evicted **after the method completes successfully**.

## Execution sequence

For a successful role update, the sequence is:

```text
1. Enter updateRole()
2. Validate the request
3. Load the user
4. Change the role
5. Save the user in the database
6. Return successfully from the method
7. Remove the user ID from the usersById cache
```

The same behavior applies to the profile update method.

An explicit version can be written as:

```java
@CacheEvict(
        cacheNames = "usersById",
        key = "#id",
        beforeInvocation = false
)
public User updateRole(Long id, UpdateRoleRequest request) {
    // ...
}
```

## If the method throws an exception

For example, if the role is invalid:

```java
throw new IllegalArgumentException("Role must be USER or ADMIN.");
```

the sequence is:

```text
1. Enter updateRole()
2. Validate the request
3. Validation fails
4. An exception is thrown
5. Cache eviction does not happen
```

The existing cache entry remains because the database was not changed. This is normally the correct behavior.

## Using `beforeInvocation = true`

The annotation can be configured as:

```java
@CacheEvict(
        cacheNames = "usersById",
        key = "#id",
        beforeInvocation = true
)
public User updateRole(Long id, UpdateRoleRequest request) {
    // ...
}
```

The execution sequence then becomes:

```text
1. Remove the user ID from the cache
2. Execute updateRole()
3. Attempt to update the database
```

With this setting, the cache is removed even if the method fails.

This may be useful when:

- The method can partially change data before throwing.
- The operation is not transactional.
- You always want to remove potentially stale data.
- Another process may modify the same data during the method.

However, if the database update fails, the old database value remains while the cache has already been removed. The next read must query the database again.

## Why the current default is appropriate

For profile and role updates:

```java
@CacheEvict(cacheNames = "usersById", key = "#id")
```

the default behavior is appropriate because:

- Validation occurs before the database save.
- Failed validation does not remove a valid cache entry.
- The cache is removed after a successful database update.
- The next `getUserById(id)` call loads the updated user and caches it again.

## Transaction consideration

If the method is transactional:

```java
@Transactional
@CacheEvict(cacheNames = "usersById", key = "#id")
public User updateRole(Long id, UpdateRoleRequest request) {
    // ...
}
```

cache eviction is associated with method completion. Depending on transaction configuration, method completion and database commit may not occur at exactly the same point.

For strict cache/database consistency, cache eviction can be coordinated with the transaction commit. For the current simple user-service update flow, the normal `@CacheEvict` behavior is sufficient.

## Recommended annotations

```java
@CacheEvict(
        cacheNames = "usersById",
        key = "#id",
        beforeInvocation = false
)
public User updateProfile(Long id, UpdateProfileRequest request) {
    // update profile
}

@CacheEvict(
        cacheNames = "usersById",
        key = "#id",
        beforeInvocation = false
)
public User updateRole(Long id, UpdateRoleRequest request) {
    // update role
}
```

## Summary

| Configuration | Cache eviction timing |
|---|---|
| `beforeInvocation = false` | After successful method completion |
| `beforeInvocation = true` | Before method execution |
| Attribute omitted | Defaults to `beforeInvocation = false` |

For the current user profile and role update methods, keeping `beforeInvocation = false` is the recommended choice.
