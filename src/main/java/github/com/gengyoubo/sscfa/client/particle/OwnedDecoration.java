package github.com.gengyoubo.sscfa.client.particle;

import java.util.UUID;

/** 粒子归属标记：带主人 UUID 的装饰性粒子。 */
public interface OwnedDecoration {
    void ssca$setDecorationOwner(UUID owner);
}
