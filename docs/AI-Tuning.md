# AI Tuning Guide

BattleForge AI now supports comprehensive tuning through configuration files, allowing you to experiment with different AI behaviors and strategies without modifying code.

## Quick Start

1. **Copy the sample config:**
   ```bash
   cp config/ai-tuning.edn config/my-aggressive-ai.edn
   ```

2. **Edit the values** in your config file to tune AI behavior

3. **Run battles** - the AI will automatically load your config

## Configuration Location

The AI looks for configuration files in this order:
1. `config/ai-tuning.edn` (primary config file)
2. If not found, uses built-in defaults

## Key Parameters Explained

### 🎯 **Threat Assessment**
```edn
:moderate-threat-threshold 2.0   ; When opponent delta advantage becomes concerning
:high-threat-threshold 3.0       ; When opponent delta advantage becomes critical
```

**Higher values** = AI tolerates more opponent advantage before reacting
**Lower values** = AI reacts more quickly to opponent threats

### ⚔️ **Strategic Aggression**
```edn
:opponent-potential-multiplier 1.5           ; How much better we need to be vs opponent
:high-threat-advantage-threshold 1.5         ; Required advantage under high threat
:moderate-threat-advantage-threshold 1.2     ; Required advantage under moderate threat
:low-threat-advantage-threshold 1.0          ; Required advantage under low threat
```

**Higher values** = AI needs bigger advantages to make aggressive moves
**Lower values** = AI makes aggressive moves with smaller advantages

### 🃏 **Card Play Requirements**
```edn
:min-cards-for-hand-advantage 2   ; Min cards to switch for hand advantage
:min-cards-for-house-switch 3     ; Min cards to switch houses
```

**Higher values** = AI requires more cards before switching strategies
**Lower values** = AI switches strategies more readily

### 💎 **Amber Control Heuristics**
```edn
:amber-control-action-with-pips 1.0      ; How much amber control actions provide
:amber-control-artifact 0.5              ; How much amber control artifacts provide
:amber-control-creature-with-pips 0.5    ; How much amber control creatures provide
```

**Higher values** = AI prioritizes amber control strategy more
**Lower values** = AI prioritizes other strategies over amber control

## Pre-Made Configurations

### 🛡️ **Defensive AI** (Conservative)
```edn
{:moderate-threat-threshold 1.5
 :high-threat-threshold 2.5
 :high-threat-advantage-threshold 1.8
 :moderate-threat-advantage-threshold 1.5
 :low-threat-advantage-threshold 1.2
 :min-cards-for-hand-advantage 3
 :min-cards-for-house-switch 4
 :battleline-advantage-vs-high-threat 3.0
 ;; ... other defaults
}
```

### ⚡ **Aggressive AI** (Risk-taking)
```edn
{:moderate-threat-threshold 3.0
 :high-threat-threshold 4.5
 :high-threat-advantage-threshold 1.2
 :moderate-threat-advantage-threshold 1.0
 :low-threat-advantage-threshold 0.8
 :min-cards-for-hand-advantage 1
 :min-cards-for-house-switch 2
 :battleline-advantage-vs-high-threat 1.5
 ;; ... other defaults
}
```

### 🎨 **Amber Control Focused**
```edn
{:amber-control-action-with-pips 1.5
 :amber-control-artifact 1.0
 :amber-control-creature-with-pips 1.0
 :opponent-potential-multiplier 1.2
 ;; ... other defaults
}
```

### 🏰 **Battleline Focused**
```edn
{:high-threat-delta-threshold 5.0
 :moderate-threat-delta-threshold 4.5
 :low-threat-delta-threshold 4.0
 :battleline-advantage-vs-high-threat 1.0
 :min-cards-for-hand-advantage 4
 ;; ... other defaults
}
```

## Testing Different Configurations

1. **Create multiple config files:**
   ```
   config/defensive-ai.edn
   config/aggressive-ai.edn
   config/amber-focused-ai.edn
   ```

2. **Swap configs by symlinking:**
   ```bash
   ln -sf defensive-ai.edn ai-tuning.edn
   # Run battles
   ln -sf aggressive-ai.edn ai-tuning.edn  
   # Run battles again
   ```

3. **Compare results** to see which strategy works better

## Advanced Tuning Tips

### 🧪 **Iterative Improvement**
1. Start with defaults
2. Change ONE parameter at a time
3. Run multiple battles to test
4. Keep notes on win rates
5. Gradually refine based on results

### 📊 **Key Metrics to Watch**
- Win rate vs different deck types
- Average game length
- Amber generation efficiency
- House selection patterns

### 🔧 **Common Adjustments**

**AI loses too often to aggressive opponents:**
- Decrease threat thresholds
- Increase amber control values
- Decrease advantage requirements

**AI plays too defensively:**
- Increase threat thresholds  
- Decrease card count requirements
- Decrease advantage thresholds

**AI makes poor house choices:**
- Adjust delta thresholds
- Tune advantage multipliers
- Review amber potential estimates

## Schema Validation

The AI validates all parameters against a strict schema. Invalid configurations will fall back to defaults with an error message.

Required parameter types:
- All thresholds: Numbers (decimals allowed)
- All card counts: Numbers (integers recommended)
- All multipliers: Numbers (> 0)

## Performance Notes

Configuration is loaded once per game, so changes require restarting battles to take effect. This design prioritizes game performance over configuration hot-reloading.

## Examples

See `config/ai-tuning.edn` for the complete list of all tunable parameters with detailed comments and examples. 