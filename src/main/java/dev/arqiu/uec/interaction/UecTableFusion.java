package dev.arqiu.uec.interaction;

import dev.arqiu.uec.equipment.ElytraChestplate;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class UecTableFusion {
   private static final int RESULT_PICKUP_DELAY = 10;
   private static final double RESULT_POP = 0.08;
   private static final double SEARCH_INFLATE_XZ = 0.35;
   private static final double SEARCH_INFLATE_UP = 1.2;
   private static final float CHIME_VOLUME = 1.0F;
   private static final float CHIME_PITCH = 1.15F;
   private static final int PARTICLE_COUNT = 14;
   private static final double PARTICLE_SPREAD_XZ = 0.25;
   private static final double PARTICLE_SPREAD_Y = 0.12;
   private static final double PARTICLE_SPEED = 0.01;

   private UecTableFusion() {
   }

   public static void onItemTick(ItemEntity self) {
      if (!self.isRemoved()) {
         if (self.level() instanceof ServerLevel serverLevel) {
            ItemStack stack = self.getItem();
            if (stack.getCount() == 1) {
               boolean asChest = ElytraChestplate.canMergeInto(stack);
               boolean asGlider = !asChest && ElytraChestplate.isGlider(stack);
               if (asChest || asGlider) {
                  BlockPos table = tableUnder(self);
                  if (table != null) {
                     ItemEntity partner = findPartner(self, serverLevel, table, asChest);
                     if (partner != null) {
                        ItemStack chest = asChest ? stack : partner.getItem();
                        ItemStack glider = asChest ? partner.getItem() : stack;
                        ElytraChestplate.merge(chest, glider, serverLevel.registryAccess())
                           .ifPresent(merged -> fuse(serverLevel, self, partner, merged, table));
                     }
                  }
               }
            }
         }
      }
   }

   private static void fuse(ServerLevel level, ItemEntity chest, ItemEntity glider, ItemStack merged, BlockPos table) {
      double x = table.getX() + 0.5;
      double z = table.getZ() + 0.5;
      double top = table.getY() + 1.0;
      chest.discard();
      glider.discard();
      ItemEntity result = new ItemEntity(level, x, top + 0.05, z, merged);
      result.setDeltaMovement(0.0, 0.08, 0.0);
      result.setPickUpDelay(10);
      level.addFreshEntity(result);
      level.playSound(null, x, top, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.15F);
      level.sendParticles(ParticleTypes.END_ROD, x, top + 0.15, z, 14, 0.25, 0.12, 0.25, 0.01);
   }

   private static BlockPos tableUnder(ItemEntity item) {
      Level level = item.level();
      BlockPos at = item.blockPosition();
      if (isTable(level, at)) {
         return at;
      }

      BlockPos below = at.below();
      return isTable(level, below) ? below : null;
   }

   private static boolean isTable(Level level, BlockPos pos) {
      return level.getBlockState(pos).is(Blocks.SMITHING_TABLE);
   }

   private static ItemEntity findPartner(ItemEntity self, ServerLevel level, BlockPos table, boolean selfIsChest) {
      Iterator var4 = level.getEntitiesOfClass(
            ItemEntity.class, topRegion(table), other -> other != self && restsOn(other, table) && isPartner(other.getItem(), selfIsChest)
         )
         .iterator();
      return var4.hasNext() ? (ItemEntity)var4.next() : null;
   }

   private static boolean isPartner(ItemStack stack, boolean selfIsChest) {
      if (stack.getCount() != 1) {
         return false;
      } else {
         return selfIsChest ? ElytraChestplate.isGlider(stack) : ElytraChestplate.canMergeInto(stack);
      }
   }

   private static boolean restsOn(ItemEntity item, BlockPos table) {
      BlockPos at = item.blockPosition();
      return at.equals(table) || at.below().equals(table);
   }

   private static AABB topRegion(BlockPos table) {
      return new AABB(table).inflate(0.35, 1.2, 0.35);
   }
}
