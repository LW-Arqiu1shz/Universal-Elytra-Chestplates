package dev.arqiu.uec.mixin;

import dev.arqiu.uec.durability.FlightWearContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NeoForge 版专用，取代 Fabric 版的 ItemStackFlightWearMixin。
//
// Fabric 版是在 ItemStack.processDurabilityChange 的方法体里重定向对
// EnchantmentHelper.processDurabilityChange 的调用。这个锚点同时依赖两件脆弱的事：
// 一个是原版私有方法的字样名字，一个是它方法体里那条具体的调用指令。
// NeoForge 运行时给出的 ItemStack 并不是平台编译时那份 minecraft-patched.jar
// （生产包是官方混淆字节码 + binpatch/coremod/AT 现场改出来的），
// 这段耐久计算的落点和私有方法名都可能对不上，
// 于是 redirect 扫不到调用点，报 (0/1) succeeded 直接崩在注入检查上。
//
// 改成拦被调方：EnchantmentHelper.processDurabilityChange 是 public static，
// 名字和描述符在 dev/prod、26.1.2/26.3 四份产物里逐字一致，且全工程只有 ItemStack
// 一处会调它（已核实），所以在它 HEAD 处把入参 stack 换成滑翔代理栈，
// 与原来那条 @Redirect 效果等价，同时不再碰任何私有名字和调用点。
//
// 递归安全：代理栈是 readSnapshot 造出来的新实例，proxyFor(proxy) 的身份比较必不相等，
// 内层那一次调用直接走原版逻辑；再加一道 proxy != stack 的保险。
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperFlightWearMixin {
   @Inject(
      method = "processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void uec$evaluateFlightWearAsGlider(ServerLevel level, ItemStack stack, int amount, CallbackInfoReturnable<Integer> cir) {
      ItemStack proxy = FlightWearContext.proxyFor(stack);
      if (proxy != null && proxy != stack) {
         cir.setReturnValue(EnchantmentHelper.processDurabilityChange(level, proxy, amount));
      }
   }
}
