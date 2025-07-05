package de.safti.saftiSk.utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class MessagingUtils {
	
	
	public static class MessageBuilder {
		private TextComponent.Builder componentBuilder;
		private Audience audience;
		
		
		private MessageBuilder() {
			this.componentBuilder = Component.text();
			this.audience = Audience.audience();
		}
		
		public MessageBuilder recipient(Audience... audiences) {
			if(this.audience == null || this.audience == Audience.empty()) {
				this.audience = Audience.audience(audiences);
				return this;
			}
			
			audiences = Arrays.stream(audiences).filter(Objects::nonNull).toArray(Audience[]::new);
			Audience[] joined = Arrays.copyOf(audiences, audiences.length + 1);
			this.audience = Audience.audience(joined);
			return this;
		}
		
		public MessageBuilder appendInline(String text) {
			componentBuilder = componentBuilder.append(fromText(text));
			return this;
		}
		
		public MessageBuilder append(Function<TextComponent.Builder, TextComponent> generator) {
			componentBuilder = componentBuilder.append(generator.apply(Component.text()));
			return this;
		}
		
		public MessageBuilder append(String content, Function<TextComponent.Builder, TextComponent> generator) {
			TextComponent.Builder builder = fromText(content).toBuilder();
			componentBuilder = componentBuilder.append(generator.apply(builder));
			return this;
		}
		
		public MessageBuilder hoverEvent(String text) {
			componentBuilder.hoverEvent(HoverEvent.showText(fromText(text)));
			return this;
		}
		
		private TextComponent fromText(String text) {
			return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
		}
		
		public void send() {
			audience.sendMessage(componentBuilder.build());
		}
	}
	
	public static MessageBuilder builder() {
		return new MessageBuilder();
	}
}
