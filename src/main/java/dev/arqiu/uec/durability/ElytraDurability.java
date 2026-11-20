package dev.arqiu.uec.durability;

import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ElytraDurability {
   private ElytraDurability() {
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static void damage(ItemStack stack, LivingEntity entity, EquipmentSlot slot, int amount, boolean gliderFirst) {
      ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
      if (state == null) {
         stack.hurtAndBreak(amount, entity, slot);
      } else {
         if (gliderFirst) {
            int gliderRoom = state.gliderMaxDamage() - ElytraChestplate.split(stack.getDamageValue(), state)[1];
            if (gliderRoom <= 0) {
               return;
            }

            amount = Math.min(amount, gliderRoom);
         }

         int before = stack.getDamageValue();
         ItemStack proxy = gliderFirst ? ElytraChestplateState.readSnapshot(stack, entity.level().registryAccess()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
         if (proxy.isEmpty()) {
            stack.hurtAndBreak(amount, entity, slot);
         } else {
            FlightWearContext.begin(stack, proxy);
            boolean var13 = false /* VF: Semaphore variable */;

            try {
               var13 = true;
               stack.hurtAndBreak(amount, entity, slot);
               var13 = false;
            } finally {
               if (var13) {
                  FlightWearContext.end();
               }
            }

            FlightWearContext.end();
         }

         if (!stack.isEmpty() && !ElytraChestplateState.read(stack).isEmpty()) {
            int applied = stack.getDamageValue() - before;
            if (applied > 0) {
               int[] split = ElytraChestplate.split(before, state);
               if (gliderFirst) {
                  split[1] += applied;
               } else {
                  int chestMax = Math.max(0, stack.getMaxDamage() - state.gliderMaxDamage());
                  int toChest = Math.min(applied, Math.max(0, chestMax - split[0]));
                  split[0] += toChest;
                  split[1] += applied - toChest;
               }

               ElytraChestplateState.updateGliderDamage(stack, split[1]);
            }
         }
      }
   }
}
