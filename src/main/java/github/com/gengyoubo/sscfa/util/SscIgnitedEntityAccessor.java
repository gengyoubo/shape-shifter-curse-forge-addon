package github.com.gengyoubo.sscfa.util;

import java.util.UUID;

public interface SscIgnitedEntityAccessor {
	void sscAddon$setIgniterUuid(UUID uuid);

	UUID sscAddon$getIgniterUuid();
}
