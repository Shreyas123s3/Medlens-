import test from 'node:test';
import assert from 'node:assert/strict';
import { emergency, validMedicineNames } from './safety.js';

test('local emergency phrases are caught in backend defense-in-depth', () => {
  for (const phrase of ['chest pain', "I can't breathe", 'severe allergic reaction', 'fainted', 'stroke signs', 'severe bleeding', 'sudden severe headache', 'saans nahi aa rahi', 'seene mein dard', 'behosh']) assert.equal(emergency(phrase), true, phrase);
  assert.equal(emergency('mild cough for two days'), false);
});
test('unknown and prescription medicine names are removed', () => {
  assert.deepEqual(validMedicineNames(['ibuprofen', 'antibiotic', 'RxOnly'], [{name:'Ibuprofen', otc:true},{name:'RxOnly', otc:false}]), ['Ibuprofen']);
});
