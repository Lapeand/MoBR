import re
from .schemas import Profile, FoodItem

# Approximate reference values per 100 g of ready-to-eat food; always user-confirmed.
CATALOG = [
    ('Овсяная каша', ['овсян', 'овсянка', 'каша'], (88, 3, 1.7, 15)),
    ('Банан', ['банан'], (89, 1.1, .3, 23)),
    ('Борщ', ['борщ'], (49, 2, 2, 6)),
    ('Котлета', ['котлет'], (220, 15, 14, 9)),
    ('Куриная грудка', ['куриц', 'курин', 'грудк'], (165, 31, 3.6, 0)),
    ('Гречка варёная', ['греч'], (110, 4.2, 1.1, 21)),
    ('Рис варёный', ['рис'], (130, 2.7, .3, 28)),
    ('Творог 5%', ['творог'], (121, 17, 5, 1.8)),
    ('Яйцо', ['яйц', 'яйцо'], (155, 13, 11, 1.1)),
    ('Яблоко', ['яблок'], (52, .3, .2, 14)),
    ('Салат овощной', ['салат'], (35, 1, 1, 5)),
    ('Хлеб', ['хлеб'], (250, 8, 3, 49)),
    ('Кефир', ['кефир'], (53, 3, 2.5, 4)),
    ('Картофель варёный', ['карто'], (82, 2, .4, 17)),
]
KEYS = ('kcal', 'protein', 'fat', 'carbs')

def targets(profile: Profile):
    bmr = 10 * profile.weight + 6.25 * profile.height - 5 * profile.age + (5 if profile.sex == 'male' else -161)
    kcal = round(bmr * {'low': 1.2, 'moderate': 1.375, 'high': 1.725}[profile.activity] * {'lose': .85, 'maintain': 1, 'gain': 1.1}[profile.goal])
    protein = round(profile.weight * 1.6)
    fat = round(kcal * .3 / 9)
    return dict(kcal=kcal, protein=protein, fat=fat, carbs=max(0, round((kcal-protein*4-fat*9)/4)))

def totals(items):
    return {key: round(sum(float(item[key]) for item in items), 1) for key in KEYS}

def parse_catalog(text, foods):
    items = []
    # Each comma/and-separated clause gets its own weight. Unknown clauses never disappear silently.
    for clause in re.split(r'[,;+\n]|\s+и\s+|\s+с\s+', text.lower()):
        if not clause.strip():
            continue
        food = next((f for f in foods if any(re.search(r'(?<!\w)' + re.escape(alias), clause) for alias in f.aliases)), None)
        if not food:
            return []
        match = re.search(r'(\d+(?:[.,]\d+)?)\s*(?:г(?:рамм\w*)?|g|кг)\b', clause)
        grams = float(match.group(1).replace(',', '.')) if match else (60 if 'яйц' in clause else 200)
        if match and 'кг' in match.group(0):
            grams *= 1000
        if not 0 < grams <= 3000:
            return []
        items.append(FoodItem(name=food.name, weight_g=grams, source=food.source,
                             **{k: round(v * grams / 100, 1) for k, v in food.nutrients.items()}))
    return items
